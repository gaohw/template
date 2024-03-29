package com.ctsi.vip.lib.framework.utils.cache

import androidx.annotation.Nullable


/**
 * Class : IntelligentCache
 * Create by GaoHW at 2022-10-13 11:02.
 * Description:
 */
class IntelligentCache<V>(val size: Int) : Cache<String, V> {

    private var mMap: MutableMap<String, V>? = null //可将数据永久存储至内存中的存储容器
    private var mCache: LruCache<String, V>? = null //当达到最大容量时可根据 LRU 算法抛弃不合规数据的存储容器

    init {
        mMap = HashMap()
        mCache = LruCache(size)
    }

    companion object {
        @JvmStatic
        private val KEY_KEEP = "Keep="

        /**
         * 使用此方法返回的值作为 key, 可以将数据永久存储至内存中
         *
         * @param key `key`
         * @return Keep= + `key`
         */
        @JvmStatic
        fun getKeyOfKeep(key: String?): String {
            checkNotNull(key) { "key == null" }
            return KEY_KEEP + key
        }
    }

    /**
     * 将 [.mMap] 和 [.mCache] 的 `size` 相加后返回
     *
     * @return 相加后的 `size`
     */
    @Synchronized
    override fun size(): Int {
        return mMap!!.size + mCache!!.size()
    }

    /**
     * 将 [.mMap] 和 [.mCache] 的 `maxSize` 相加后返回
     *
     * @return 相加后的 `maxSize`
     */
    @Synchronized
    override fun getMaxSize(): Int {
        return (mMap?.size ?: 0) + (mCache?.getMaxSize() ?: 0)
    }

    /**
     * 如果在 `key` 中使用 [.KEY_KEEP] 作为其前缀, 则操作 [.mMap], 否则操作 [.mCache]
     *
     * @param key `key`
     * @return `value`
     */
    @Nullable
    @Synchronized
    override fun get(key: String): V? {
        return if (key.startsWith(KEY_KEEP)) {
            mMap!![key]
        } else mCache!![key]
    }

    /**
     * 如果在 `key` 中使用 [.KEY_KEEP] 作为其前缀, 则操作 [.mMap], 否则操作 [.mCache]
     *
     * @param key   `key`
     * @param value `value`
     * @return 如果这个 `key` 在容器中已经储存有 `value`, 则返回之前的 `value` 否则返回 `null`
     */
    @Nullable
    @Synchronized
    override fun put(key: String, value: V): V? {
        return if (key.startsWith(KEY_KEEP)) {
            mMap?.put(key, value)
        } else {
            mCache?.put(key, value)
        }
    }

    /**
     * 如果在 `key` 中使用 [.KEY_KEEP] 作为其前缀, 则操作 [.mMap], 否则操作 [.mCache]
     *
     * @param key `key`
     * @return 如果这个 `key` 在容器中已经储存有 `value` 并且删除成功则返回删除的 `value`, 否则返回 `null`
     */
    @Nullable
    @Synchronized
    override fun remove(key: String): V? {
        return if (key.startsWith(KEY_KEEP)) {
            mMap?.remove(key)
        } else {
            mCache?.remove(key)
        }
    }

    /**
     * 如果在 `key` 中使用 [.KEY_KEEP] 作为其前缀, 则操作 [.mMap], 否则操作 [.mCache]
     *
     * @param key `key`
     * @return `true` 为在容器中含有这个 `key`, 否则为 `false`
     */
    @Synchronized
    override fun containsKey(key: String): Boolean {
        return if (key.startsWith(KEY_KEEP)) {
            mMap?.containsKey(key) ?: false
        } else {
            mCache?.containsKey(key) ?: false
        }
    }

    /**
     * 将 [.mMap] 和 [.mCache] 的 `keySet` 合并返回
     *
     * @return 合并后的 `keySet`
     */
    @Synchronized
    override fun keySet(): Set<String>? {
        val set = mCache?.keySet()?.toMutableSet()
        mMap?.keys?.let { set?.addAll(it) }
        return set
    }

    /**
     * 清空 [.mMap] 和 [.mCache] 容器
     */
    override fun clear() {
        mCache?.clear()
        mMap?.clear()
    }
}