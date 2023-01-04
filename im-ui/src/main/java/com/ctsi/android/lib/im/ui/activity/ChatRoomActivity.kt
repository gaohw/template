package com.ctsi.android.lib.im.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ColorUtils
import com.ctsi.android.lib.im.CtsiIM
import com.ctsi.android.lib.im.bean.MessageBean
import com.ctsi.android.lib.im.bean.UserBean
import com.ctsi.android.lib.im.interfaces.MessageListener
import com.ctsi.android.lib.im.ui.R
import com.ctsi.android.lib.im.ui.adapter.MessageAdapter
import com.ctsi.android.lib.im.ui.databinding.ImActivityChatBinding

/**
 * Class : ChatRoomActivity
 * Create by GaoHW at 2023-1-3 11:39.
 * Description:
 */
open class ChatRoomActivity : AppCompatActivity(), MessageListener {

    private lateinit var mBinding: ImActivityChatBinding
    private lateinit var userBean: UserBean

    private val messageAdapter: MessageAdapter = MessageAdapter()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userBean = CtsiIM.userManager().getUserById(intent?.getStringExtra("user_id")) ?: return
        mBinding = ImActivityChatBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        window.statusBarColor = ColorUtils.getColor(R.color.im_title)

        mBinding.tvChatUsername.text = userBean.userName
        mBinding.btnBack.setOnClickListener { onBackPressed() }
        mBinding.layoutInput.setInputSendListener {
            CtsiIM.messageManager().sendTextMessage("${userBean.userId}", it)
        }
        mBinding.rvMessage.adapter = messageAdapter
        mBinding.rvMessage.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)

        CtsiIM.messageManager().setMessageListener(this)
        messageAdapter.setNewData(CtsiIM.messageManager().getMessageList("${userBean.userId}", 0))
    }

    override fun getChatId(): String? = userBean.userId

    override fun onReceiveMessage(message: MessageBean) {
        messageAdapter.addData(0, message)
        mBinding.rvMessage.scrollToPosition(0)
    }

    override fun onDestroy() {
        CtsiIM.messageManager().setMessageListener(null)
        super.onDestroy()
    }

    companion object {
        @JvmStatic
        fun chatPrivate(context: Context, userId: String) {
            val intent = Intent(context, ChatRoomActivity::class.java)
                .putExtra("user_id", userId)
            context.startActivity(intent)
        }
    }
}