package com.common.db

import io.objectbox.Box

open class BaseDB<T>(entityClass: Class<T>) {
    protected val boxFor: Box<T> = DbManager.store.boxFor(entityClass)

    /**
     * 插入单个对象
     */
    fun insert(entity: T): Long {
        return boxFor.put(entity)
    }

    /**
     * 插入多个对象
     */
    fun insert(entities: List<T>) {
        boxFor.put(entities)
    }

    /**
     * 更新单个对象 (ObjectBox 的 put 方法会覆盖现有对象)
     */
    fun update(entity: T): Long {
        return boxFor.put(entity)
    }

    /**
     * 更新多个对象
     */
    fun update(entities: List<T>) {
        boxFor.put(entities)
    }

    /**
     * 删除单个对象
     */
    fun delete(entity: T): Boolean {
        return boxFor.remove(entity)
    }

    /**
     * 根据 ID 删除对象
     */
    fun deleteById(id: Long): Boolean {
        return boxFor.remove(id)
    }

    /**
     * 删除多个对象
     */
    fun delete(entities: List<T>) {
        boxFor.remove(entities)
    }

    /**
     * 根据 ID 列表删除对象
     */
    fun deleteByIds(ids: List<Long>) {
        boxFor.removeByIds(ids)
    }

    /**
     * 删除所有对象
     */
    fun deleteAll() {
        boxFor.removeAll()
    }

    /**
     * 根据 ID 查询对象
     */
    fun getById(id: Long): T? {
        return boxFor.get(id)
    }

    /**
     * 查询所有对象
     */
    fun getAll(): List<T> {
        return boxFor.all
    }

    /**
     * 获取 Box 实例，以便进行更复杂的查询
     */
    fun getBox(): Box<T> {
        return boxFor
    }
}