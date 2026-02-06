package it.fast4x.riplay.service.experimental

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


object AppSharedScope : ViewModelStoreOwner {

    private val store = ViewModelStore()

    override val viewModelStore: ViewModelStore
        get() = store

    fun clear() {
        store.clear()
    }
}

inline fun <reified VM : ViewModel> getSharedViewModel(): VM {
    return ViewModelProvider(AppSharedScope)[VM::class.java]
}
