package com.ctsi.android.lib.im.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.TimeUtils
import com.bumptech.glide.Glide
import com.ctsi.android.lib.im.CtsiIM
import com.ctsi.android.lib.im.bean.MessageBean
import com.ctsi.android.lib.im.enums.Def
import com.ctsi.android.lib.im.ui.R
import com.ctsi.android.lib.im.ui.adapter.viewholder.MsgFileViewHolder
import com.ctsi.android.lib.im.ui.adapter.viewholder.MsgImageViewHolder
import com.ctsi.android.lib.im.ui.adapter.viewholder.MsgTextViewHolder
import com.ctsi.android.lib.im.ui.adapter.viewholder.MsgViewHolder
import com.ctsi.android.lib.im.ui.utils.inflate
import com.ctsi.android.lib.im.utils.DateUtils
import kotlin.math.abs

/**
 * Class : MessageAdapter
 * Create by GaoHW at 2023-1-3 15:01.
 * Description:
 */
class MessageAdapter : RecyclerView.Adapter<MsgViewHolder>() {

    private val ITEM_TEXT_SEND = 1
    private val ITEM_TEXT_RECEIVE = 2
    private val ITEM_IMAGE_SEND = 3
    private val ITEM_IMAGE_RECEIVE = 4
    private val ITEM_FILE_SEND = 5
    private val ITEM_FILE_RECEIVE = 6

    private val messageData: MutableList<MessageBean> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MsgViewHolder {
        return when (viewType) {
            ITEM_TEXT_SEND -> MsgTextViewHolder(parent.inflate(R.layout.im_item_message_text_right))
            ITEM_TEXT_RECEIVE -> MsgTextViewHolder(parent.inflate(R.layout.im_item_message_text_left))
            ITEM_IMAGE_SEND -> MsgImageViewHolder(parent.inflate(R.layout.im_item_message_image_right))
            ITEM_IMAGE_RECEIVE -> MsgImageViewHolder(parent.inflate(R.layout.im_item_message_image_left))
            ITEM_FILE_SEND -> MsgFileViewHolder(parent.inflate(R.layout.im_item_message_file_right))
            ITEM_FILE_RECEIVE -> MsgFileViewHolder(parent.inflate(R.layout.im_item_message_file_left))
            else -> throw IllegalArgumentException("viewtype $viewType is unknown!!!")
        }
    }

    override fun onBindViewHolder(holder: MsgViewHolder, position: Int) {
        val message = getItemOrNull(position)
        if (message != null) {
            val user = CtsiIM.userManager().getUserById(message.msgFrom)
            Glide.with(holder.root).load(user?.userAvatar).placeholder(R.drawable.im_ic_avatar_default)
                .into(holder.ivAvatar)
            holder.tvName.text = user?.userName
            holder.updateMessage(position, message)

            val pre = getItemOrNull(position + 1)
            val formatTime = formatMessageTime(message, pre)
            holder.tvTime.text = formatTime
            holder.tvTime.visibility = if (formatTime != null) View.VISIBLE else View.GONE
        } else {
            //do something
        }
    }

    fun setNewData(data: List<MessageBean>?) {
        messageData.clear()
        if (!data.isNullOrEmpty()) {
            messageData.addAll(data)
        }
        notifyDataSetChanged()
    }

    fun addData(message: MessageBean?) {
        if (message != null) {
            val index = itemCount
            messageData.add(message)
            notifyItemRangeChanged(index, 1)
        }
    }

    fun addData(position: Int, message: MessageBean?) {
        if (message != null) {
            messageData.add(position, message)
            notifyItemInserted(position)
        }
    }

    fun addAllData(data: List<MessageBean>?) {
        if (!data.isNullOrEmpty()) {
            val index = itemCount
            messageData.addAll(data)
            notifyItemRangeChanged(index, data.size)
        }
    }

    fun addAllData(position: Int, data: List<MessageBean>?) {
        if (!data.isNullOrEmpty()) {
            messageData.addAll(position, data)
            notifyItemRangeChanged(position, data.size)
        }
    }

    fun getItemOrNull(position: Int) = messageData.getOrNull(position)

    private fun formatMessageTime(cur: MessageBean, pre: MessageBean?): String? {
        try {
            val date = TimeUtils.millis2Date(cur.msgTime!!.toLong())
            val need =
                if (pre == null) true
                else {
                    val datePre = TimeUtils.millis2Date(pre.msgTime!!.toLong())
                    abs(date.time - datePre.time) > 300000
                }
            if (need) {
                return DateUtils.formatDate(date)
            }
            return null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun getItemCount(): Int = messageData.size

    override fun getItemViewType(position: Int): Int {
        val message = messageData.getOrNull(position)
        return when (message?.msgType) {
            Def.TYPE_TEXT -> if (message.isSelf()) ITEM_TEXT_SEND else ITEM_TEXT_RECEIVE
            Def.TYPE_IMAGE -> if (message.isSelf()) ITEM_IMAGE_SEND else ITEM_IMAGE_RECEIVE
            Def.TYPE_FILE -> if (message.isSelf()) ITEM_FILE_SEND else ITEM_FILE_RECEIVE
            else -> 0
        }
    }
}