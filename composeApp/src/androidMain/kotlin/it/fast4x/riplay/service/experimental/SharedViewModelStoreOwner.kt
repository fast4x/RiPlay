package it.fast4x.riplay.service.experimental

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner


object AppSharedScope : ViewModelStoreOwner {

    private val store = ViewModelStore()

    override val viewModelStore: ViewModelStore
        get() = store

    fun clear() {
        store.clear()
    }
}