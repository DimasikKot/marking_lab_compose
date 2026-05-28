package com.koolda.marking_lab_compose

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform