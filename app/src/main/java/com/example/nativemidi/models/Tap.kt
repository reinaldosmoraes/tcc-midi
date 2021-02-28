package com.example.nativemidi.models

import java.util.*
import kotlin.collections.ArrayList

class Tap(var hand: Hand,
          var intensity: Int?,
          var tapTime: Date? = null,
          var interval: Long? = null,
          var isAccented: Boolean?) {

    companion object {

        @JvmStatic
        fun getParadiddlePattern(): ArrayList<Tap> {
            return arrayListOf(
                    Tap(Hand.RIGHT, null, null, null, true),
                    Tap(Hand.LEFT, null, null, null, false),
                    Tap(Hand.RIGHT, null, null, null, false),
                    Tap(Hand.RIGHT, null, null, null, false),
                    Tap(Hand.LEFT, null, null, null, true),
                    Tap(Hand.RIGHT, null, null, null, false),
                    Tap(Hand.LEFT, null, null, null, false),
                    Tap(Hand.LEFT, null, null, null, false)
            )
        }

    }
}