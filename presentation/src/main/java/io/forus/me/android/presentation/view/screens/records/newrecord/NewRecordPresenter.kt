package io.forus.me.android.presentation.view.screens.records.newrecord

import com.ocrv.ekasui.mrm.ui.loadRefresh.LRPresenter
import com.ocrv.ekasui.mrm.ui.loadRefresh.LRViewState
import com.ocrv.ekasui.mrm.ui.loadRefresh.PartialChange
import io.forus.me.android.domain.models.records.NewRecordRequest
import io.forus.me.android.domain.models.records.RecordCategory
import io.forus.me.android.domain.models.records.RecordType
import io.forus.me.android.domain.repository.records.RecordsRepository
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers

class NewRecordPresenter constructor(private val recordRepository: RecordsRepository) : LRPresenter<NewRecordModel, NewRecordModel, NewRecordView>() {

    var request: NewRecordRequest? = null;

    override fun initialModelSingle(): Single<NewRecordModel> = Single.zip(
            Single.fromObservable(recordRepository.getRecordTypes()),
            Single.fromObservable(recordRepository.getCategories()),
            BiFunction { types : List<RecordType>, categories: List<RecordCategory> -> NewRecordModel(types = types, categories = categories)}
    )


    override fun NewRecordModel.changeInitialModel(i: NewRecordModel): NewRecordModel = i.copy()


    override fun bindIntents() {

        var observable = Observable.merge(

                loadRefreshPartialChanges(),
                Observable.merge(
                        intent { it.selectCategory() }
                                .map {  NewRecordPartialChanges.SelectCategory(it) },
                        intent { it.selectType() }
                                .map {  NewRecordPartialChanges.SelectType(it) },
                        intent { it.setValue() }
                                .map {  NewRecordPartialChanges.SetValue(it) }

                ),
                intent { it.createRecord() }
                        .switchMap {
                            recordRepository.newRecord(request!!)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .map<PartialChange> {
                                        NewRecordPartialChanges.CreateRecordEnd(it)
                                    }
                                    .onErrorReturn {
                                        NewRecordPartialChanges.CreateRecordError(it)
                                    }
                                    .startWith(NewRecordPartialChanges.CreateRecordStart(request!!))
                        }
        );


        val initialViewState = LRViewState(
                false,
                null,
                false,
                false,
                null,
                false,
                NewRecordModel())

        subscribeViewState(
                observable.scan(initialViewState, this::stateReducer)
                        .observeOn(AndroidSchedulers.mainThread()),
                NewRecordView::render)

//        val observable = loadRefreshPartialChanges()
//        val initialViewState = LRViewState(false, null, false, false, null, MapModel("", "" ))
//        subscribeViewState(observable.scan(initialViewState, this::stateReducer).observeOn(AndroidSchedulers.mainThread()),MapView::render)
    }

    override fun stateReducer(vs: LRViewState<NewRecordModel>, change: PartialChange): LRViewState<NewRecordModel> {
        var result : LRViewState<NewRecordModel>? = null
        if (change !is NewRecordPartialChanges)
            result =  super.stateReducer(vs, change)

        when (change) {
            is NewRecordPartialChanges.CreateRecordEnd -> result = vs.copy(closeScreen = true, model = vs.model.copy(sendingCreateRecord = false, sendingCreateRecordError = null))
            is NewRecordPartialChanges.CreateRecordStart -> result = vs.copy(model = vs.model.copy(sendingCreateRecord = true, sendingCreateRecordError = null))
            is NewRecordPartialChanges.CreateRecordError -> result = vs.copy(model = vs.model.copy(sendingCreateRecord = false, sendingCreateRecordError = change.error))
            is NewRecordPartialChanges.SelectCategory -> result = vs.copy(model = vs.model.copy(item = vs.model.item.copy(category = change.category)))
            is NewRecordPartialChanges.SelectType -> result = vs.copy(model = vs.model.copy(item = vs.model.item.copy(recordType = change.type)))
            is NewRecordPartialChanges.SetValue -> result = vs.copy(model = vs.model.copy(item = vs.model.item.copy(value = change.value)))

        }


        request = result?.model?.item

        return  result!!


    }
}