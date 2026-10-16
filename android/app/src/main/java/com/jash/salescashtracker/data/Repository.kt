package com.jash.salescashtracker.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant

enum class SyncState { Checking, SignedOut, Syncing, Synced, Offline, Error }

/**
 * Talks to the same Firestore layout as the web and Windows apps:
 *   users/{uid}/bookings/{id}, users/{uid}/cash/{id}, users/{uid}/meta/config
 * Every write carries a numeric "_ts" (milliseconds); deletes are stored as
 * {id, deleted:true, _ts} so the other apps remove the record too.
 * Firestore keeps its own offline cache, so the app works without internet.
 */
object Repository {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    private val _user = MutableStateFlow<FirebaseUser?>(null)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings.asStateFlow()

    private val _cash = MutableStateFlow<List<CashEntry>>(emptyList())
    val cash: StateFlow<List<CashEntry>> = _cash.asStateFlow()

    private val _lists = MutableStateFlow(Lists())
    val lists: StateFlow<Lists> = _lists.asStateFlow()

    private val _settings = MutableStateFlow(BusinessSettings())
    val settings: StateFlow<BusinessSettings> = _settings.asStateFlow()

    private val _sync = MutableStateFlow(SyncState.Checking)
    val sync: StateFlow<SyncState> = _sync.asStateFlow()

    private val _syncError = MutableStateFlow("")
    val syncError: StateFlow<String> = _syncError.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private var listeners = mutableListOf<ListenerRegistration>()
    private val pending = mutableMapOf<String, Boolean>()
    private val fromCache = mutableMapOf<String, Boolean>()
    private var started = false

    fun start() {
        if (started) return
        started = true
        auth.addAuthStateListener { a ->
            val u = a.currentUser
            if (u?.uid != _user.value?.uid || (u == null && _sync.value == SyncState.Checking)) {
                _user.value = u
                attach(u)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun attach(u: FirebaseUser?) {
        listeners.forEach { it.remove() }
        listeners.clear()
        pending.clear(); fromCache.clear()
        if (u == null) {
            _bookings.value = emptyList(); _cash.value = emptyList()
            _lists.value = Lists(); _settings.value = BusinessSettings()
            _loaded.value = false
            _sync.value = SyncState.SignedOut
            return
        }
        _sync.value = SyncState.Syncing
        val base = db.collection("users").document(u.uid)

        listeners += base.collection("bookings").addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) return@addSnapshotListener fail(err)
            if (snap == null) return@addSnapshotListener
            _bookings.value = snap.documents.mapNotNull { d ->
                val m = d.data ?: return@mapNotNull null
                if (m["deleted"] == true) null else Booking.fromMap(d.id, m)
            }
            _loaded.value = true
            track("bookings", snap)
        }
        listeners += base.collection("cash").addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) return@addSnapshotListener fail(err)
            if (snap == null) return@addSnapshotListener
            _cash.value = snap.documents.mapNotNull { d ->
                val m = d.data ?: return@mapNotNull null
                if (m["deleted"] == true) null else CashEntry.fromMap(d.id, m)
            }
            track("cash", snap)
        }
        listeners += base.collection("meta").document("config").addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) return@addSnapshotListener fail(err)
            if (snap == null) return@addSnapshotListener
            if (snap.exists()) {
                _lists.value = Lists.fromMap(snap.get("lists") as? Map<String, Any?>)
                _settings.value = BusinessSettings.fromMap(snap.get("settings") as? Map<String, Any?>)
            }
            pending["meta"] = snap.metadata.hasPendingWrites()
            fromCache["meta"] = snap.metadata.isFromCache
            updateSync()
        }
    }

    private fun track(key: String, snap: QuerySnapshot) {
        pending[key] = snap.metadata.hasPendingWrites()
        fromCache[key] = snap.metadata.isFromCache
        updateSync()
    }

    private fun updateSync() {
        if (_user.value == null) return
        _syncError.value = ""
        _sync.value = when {
            pending.values.any { it } -> if (fromCache.values.any { it }) SyncState.Offline else SyncState.Syncing
            fromCache.values.any { it } -> SyncState.Offline
            else -> SyncState.Synced
        }
    }

    private fun fail(e: Exception) {
        _syncError.value = e.message ?: e.toString()
        _sync.value = SyncState.Error
    }

    // ---------- Auth ----------
    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun signUp(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun signOut() = auth.signOut()

    // ---------- Writes ----------
    private fun now() = System.currentTimeMillis()
    private fun nowIso() = Instant.now().toString()
    private fun col(name: String) =
        db.collection("users").document(_user.value?.uid ?: error("Not signed in")).collection(name)

    fun saveBooking(b: Booking) {
        val isNew = _bookings.value.none { it.id == b.id }
        val rec = b.copy(
            createdAt = if (isNew && b.createdAt.isBlank()) nowIso() else b.createdAt,
            updatedAt = if (isNew) b.updatedAt else nowIso(),
        )
        col("bookings").document(rec.id).set(rec.toMap() + ("_ts" to now()))
        if (rec.agent.isNotBlank()) addToList("agents", rec.agent)
    }

    fun deleteBooking(id: String) {
        col("bookings").document(id).set(mapOf("id" to id, "deleted" to true, "_ts" to now()))
    }

    fun saveCash(c: CashEntry) {
        val isNew = _cash.value.none { it.id == c.id }
        val rec = if (isNew && c.createdAt.isBlank()) c.copy(createdAt = nowIso()) else c
        col("cash").document(rec.id).set(rec.toMap() + ("_ts" to now()))
        if (rec.agent.isNotBlank()) addToList("agents", rec.agent)
    }

    fun deleteCash(id: String) {
        col("cash").document(id).set(mapOf("id" to id, "deleted" to true, "_ts" to now()))
    }

    fun saveLists(l: Lists) {
        _lists.value = l
        saveMeta()
    }

    fun saveSettings(s: BusinessSettings) {
        _settings.value = s
        saveMeta()
    }

    private fun saveMeta() {
        db.collection("users").document(_user.value?.uid ?: return).collection("meta").document("config")
            .set(mapOf("settings" to _settings.value.toMap(), "lists" to _lists.value.toMap(), "_ts" to now()))
    }

    private fun addToList(which: String, value: String) {
        val l = _lists.value
        if (which == "agents" && value !in l.agents) saveLists(l.copy(agents = l.agents + value))
    }

    /** Writes a whole backup (from the web/desktop "Save backup file") into the account. */
    fun restore(bookings: List<Booking>, cash: List<CashEntry>, lists: Lists?, settings: BusinessSettings?) {
        val uid = _user.value?.uid ?: return
        val base = db.collection("users").document(uid)
        val ts = now()
        val ops = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any?>>>()
        bookings.forEach { ops += base.collection("bookings").document(it.id) to (it.toMap() + ("_ts" to ts)) }
        cash.forEach { ops += base.collection("cash").document(it.id) to (it.toMap() + ("_ts" to ts)) }
        ops.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (ref, data) -> batch.set(ref, data) }
            batch.commit()
        }
        if (lists != null) _lists.value = lists
        if (settings != null) _settings.value = settings
        if (lists != null || settings != null) saveMeta()
    }
}
