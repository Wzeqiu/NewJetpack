package com.common.db.dao

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity(useNoArgConstructor = true)
class NewUser {
    @Id
    var id: Long = 0

    val name: String = ""

    val age:Int=0
}