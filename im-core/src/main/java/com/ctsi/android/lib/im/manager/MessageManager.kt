package com.ctsi.android.lib.im.manager

import com.blankj.utilcode.util.ThreadUtils
import com.blankj.utilcode.util.TimeUtils
import com.ctsi.android.lib.im.bean.MessageBean
import com.ctsi.android.lib.im.bean.ChatBean
import com.ctsi.android.lib.im.bean.UserBean
import com.ctsi.android.lib.im.core.IMWebSocketCallback
import com.ctsi.android.lib.im.core.IMConnectManager
import com.ctsi.android.lib.im.core.IMessageManager
import com.ctsi.android.lib.im.enums.Def
import com.ctsi.android.lib.im.interfaces.MessageListener
import com.ctsi.android.lib.im.interfaces.MessageChatListener
import org.json.JSONObject
import java.util.*

/**
 * Class : MessageManager
 * Create by GaoHW at 2022-12-30 8:25.
 * Description: 消息管理类
 */
internal object MessageManager : IMessageManager, IMWebSocketCallback {

    private const val WEB_SOCKET = "ws://27.128.172.122:8000/webSocket/"
    private const val WEB_SOCKET_SYSTEM = "ctsiapp"
    private const val PAGE_SIZE = 20

    private val messageCache = mutableMapOf<String, LinkedList<MessageBean>>()
    private val messageChatCache = LinkedList<ChatBean>()

    private var messageSocket: String? = null
    private var messageListener: MessageListener? = null
    private var messageChatListener: MessageChatListener? = null

    fun connect() {
        //连接
        val user = UserManager.currentUser()
        if (user != null) {
            val socket = WEB_SOCKET + WEB_SOCKET_SYSTEM + "/${user.userId}"
            messageSocket = socket
            IMConnectManager.connect(socket)
            IMConnectManager.registerCallback(socket, this)
        }
        //todo 获取历史信息
    }

    fun close(userId: String?) {
        if (userId.isNullOrEmpty()) {
            IMConnectManager.closeAll()
        } else {
            IMConnectManager.close(WEB_SOCKET + WEB_SOCKET_SYSTEM + "/${userId}")
        }
        messageCache.clear()
        messageChatCache.clear()
        messageListener = null
        messageChatListener = null
    }

    override fun sendTextMessage(to: String, content: String) = doUploadFileAndSendMessage(Def.TYPE_TEXT, to, content)

    override fun sendImageMessage(to: String, path: String) = doUploadFileAndSendMessage(Def.TYPE_IMAGE, to, path)

    override fun sendFileMessage(to: String, path: String) = doUploadFileAndSendMessage(Def.TYPE_FILE, to, path)

    //发送信息
    private fun doUploadFileAndSendMessage(@Def.MessageType type: String, to: String, content: String) {
        if (type == Def.TYPE_IMAGE || type == Def.TYPE_FILE) {
            //上传文件
        }
        val user = UserManager.currentUser()
        if (user != null) {
            val message = MessageBean(type).apply {
                msgFrom = user.userId
                msgTo = to
                msgContent = content
                msgTime = TimeUtils.millis2String(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss")
                readStatus = 1
            }
            addUser(to)
            addMessage(to, message)
            addMessageChat(to, message)
            if (user.userId != to) {
                IMConnectManager.sendMessage("$messageSocket", message.toSendMessage())
            }
        }
    }

    override fun getMessageChatList(): MutableList<ChatBean>? = messageChatCache

    override fun getMessageList(id: String, page: Int): MutableList<MessageBean>? = messageCache[id]

    override fun getUnreadCount(): Int = messageChatCache.sumOf { it.unreadCount }

    override fun onReceiveMessage(msg: String) {
        try {
            val json = JSONObject(msg)
            if (json.optString("system") == WEB_SOCKET_SYSTEM) {
                val message = MessageBean()
                message.msgType = Def.TYPE_TEXT
                message.msgContent = json.optString("content")
                message.msgFrom = json.optString("from")
                message.msgTo = json.optString("to")
                message.msgTime = json.optString("sendDate")

                addUser(message.msgFrom)
                addMessage("${message.msgFrom}", message)
                addMessageChat("${message.msgFrom}", message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRetryOut() {
        super.onRetryOut()
        messageListener?.onDisconnect()
        messageChatListener?.onDisconnect()
    }

    private fun addUser(id: String?): UserBean? {
        if (id.isNullOrEmpty()) {
            return null
        }
        var user = UserManager.getUserById(id)
        if (user == null) {
            user = UserBean().apply {
                userId = id
                userName = id
            }
            UserManager.addUser(user)
        }
        return user
    }

    private fun addMessage(id: String, message: MessageBean) {
        var messageList = messageCache[id]
        if (messageList == null) {
            messageList = LinkedList<MessageBean>()
                .apply { add(message) }
            messageCache[id] = messageList
        } else {
            messageList.add(0, message)
        }
        if (messageListener?.getChatId() == id) {
            message.readStatus = 1
            runOnUiThread { messageListener?.onReceiveMessage(message) }
        }
    }

    private fun addMessageChat(id: String, message: MessageBean) {
        var chat = messageChatCache.find { it.user?.userId == id }
        if (chat == null) {
            chat = ChatBean().apply {
                this.user = UserManager.getUserById(id)
                this.message = message
                this.unreadCount += if (message.readStatus == 0) 1 else 0
            }
            messageChatCache.add(0, chat)
            runOnUiThread { messageChatListener?.onReceiveMessage(chat, -1) }
        } else {
            chat.user = UserManager.getUserById(id)
            chat.message = message
            chat.unreadCount += if (message.readStatus == 0) 1 else 0
            //交换位置
            val index = messageChatCache.indexOf(chat)
            if (index > 0) {
                messageChatCache.remove(chat)
                messageChatCache.add(0, chat)
            }
            runOnUiThread { messageChatListener?.onReceiveMessage(chat, index) }
        }
    }

    private inline fun runOnUiThread(crossinline block: () -> Unit) {
        ThreadUtils.runOnUiThread { block.invoke() }
    }

    override fun setMessageListener(listener: MessageListener?) {
        messageListener = listener
    }

    override fun setMessageChatListener(listener: MessageChatListener?) {
        messageChatListener = listener
    }

    override fun requestChatList(page: Int) {
        runOnUiThread { messageChatListener?.onChatList(page, messageChatCache) }
    }

    override fun requestMessageInChat(chatId: String, page: Int) {
        messageChatCache.find { it.user?.userId == chatId }?.unreadCount = 0
        val data = messageCache[chatId]
        val result =
            if (data != null) {
                val start = PAGE_SIZE * page
                val end = if (start + PAGE_SIZE > data.size) data.size else start + PAGE_SIZE
                if (start < end) data.subList(start, end) else null
            } else null
        runOnUiThread { messageListener?.onMessageList(page, result) }
    }
}