package it.fast4x.riplay.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import it.fast4x.riplay.data.models.Recommendation

@Dao
interface RecommendationDao {

    @Query("""
    SELECT * FROM recommendation 
    WHERE userId = :userId AND strategyId = :strategyId
""")
    suspend fun getActiveAndConsumed(userId: String, strategyId: String): List<Recommendation>

    /**
     * Recupera le recommendations attive (non consumate di recente, non rejected).
     * @param userId ID utente
     * @param strategyId ID strategia
     * @param consumedCutoffMs timestamp: esclude i consumati dopo questa data (14gg fa)
     * @param rejectedCutoffMs timestamp: esclude i rejected dopo questa data (30gg fa)
     * @param limit numero massimo di risultati
     */
    @Query("""
    SELECT r.* FROM recommendation r
    WHERE r.userId = :userId
      AND r.strategyId = :strategyId
      AND (r.consumed = 0 OR r.consumedAt IS NULL OR r.consumedAt < :consumedCutoffMs)
      AND (r.rejectedAt IS NULL OR r.rejectedAt < :rejectedCutoffMs)
    ORDER BY r.score DESC
    LIMIT :limit
""")
    suspend fun getActiveRecommendations(
        userId: String,
        strategyId: String,
        consumedCutoffMs: Long,
        rejectedCutoffMs: Long,
        limit: Int
    ): List<Recommendation>

    /**
     * Marca come consumato (utente ha aperto il brano/album/artista).
     */
    @Query("""
    UPDATE recommendation 
    SET consumed = 1, consumedAt = :now 
    WHERE userId = :userId AND songId = :itemId AND strategyId = :strategyId
""")
    suspend fun markConsumed(userId: String, strategyId: String, itemId: String, now: Long)

    /**
     * Marca come rejected (utente ha detto "Non mi interessa").
     * Aggiorna TUTTE le strategie per quell'item (non solo una).
     */
    @Query("""
    UPDATE recommendation 
    SET rejectedAt = :now 
    WHERE userId = :userId AND songId = :itemId
""")
    suspend fun markRejected(userId: String, itemId: String, now: Long)

    /**
     * Marca come rejected per album/artist ID (quando songId è null).
     */
    @Query("""
    UPDATE recommendation 
    SET rejectedAt = :now 
    WHERE userId = :userId AND songId = :itemIdentifier
""")
    suspend fun markRejectedByIdentifier(userId: String, itemIdentifier: String, now: Long)

    @Query("DELETE FROM recommendation WHERE userId = :userId AND strategyId = :strategyId")
    suspend fun deleteByStrategy(userId: String, strategyId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM recommendation WHERE userId = :userId AND strategyId = :strategyId AND songId = :itemId)")
    suspend fun checkIfExists(userId: String, strategyId: String, itemId: String): Boolean

    @Query("SELECT consumed FROM recommendation WHERE userId = :userId AND strategyId = :strategyId AND songId = :itemId")
    suspend fun checkIfConsumed(userId: String, strategyId: String, itemId: String): Boolean?

    /**
     * Recupera gli ID (songId) consumati o rifiutati di recente.
     * Usato per passare excludedIds alle strategie.
     */
    @Query("""
    SELECT songId FROM recommendation 
    WHERE userId = :userId
      AND (
        (consumed = 1 AND consumedAt IS NOT NULL AND consumedAt > :consumedCutoffMs)
        OR (rejectedAt IS NOT NULL AND rejectedAt > :rejectedCutoffMs)
      )
""")
    suspend fun getRecentlyConsumedOrRejectedIds(
        userId: String,
        consumedCutoffMs: Long,
        rejectedCutoffMs: Long
    ): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecommendation(items: List<Recommendation>)


}