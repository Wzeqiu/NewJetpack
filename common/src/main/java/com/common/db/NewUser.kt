package com.common.db

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Uid

@Entity(useNoArgConstructor = true)
class NewUser {
    @Id
    var id: Long = 0

    val name: String = ""

    val age:Int=0
}