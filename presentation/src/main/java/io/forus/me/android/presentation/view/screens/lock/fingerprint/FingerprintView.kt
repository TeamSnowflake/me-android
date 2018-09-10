package io.forus.me.android.presentation.view.screens.lock.fingerprint

import io.forus.me.android.presentation.view.base.lr.LRView

interface FingerprintView : LRView<FingerprintModel> {

    fun exit(): io.reactivex.Observable<Unit>

}