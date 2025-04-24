package com.common.db.dao

import com.common.db.BaseDB
import com.common.db.NewUser_

/**
 * NewUser 表的数据访问对象 (DAO)
 */
class NewUserDao : BaseDB<NewUser>(NewUser::class.java) {
    // 可以在这里添加针对 NewUser 表的特定查询方法
    // 例如：根据姓名查找用户
    // fun findByName(name: String): List<NewUser> {
    //     return box.query(NewUser_.name.equal(name)).build().find()
    // }


    fun queryAllByUser(): List<NewUser> {
        return boxFor.query().equal(NewUser_.id, 1).build().find()
    }
}