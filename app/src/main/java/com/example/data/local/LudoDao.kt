package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LudoDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observeProfile(): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: PlayerProfileEntity)

    @Query("SELECT * FROM match_history ORDER BY timestampMs DESC LIMIT 50")
    fun observeMatchHistory(): Flow<List<MatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchHistory(item: MatchHistoryEntity)

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsOnce(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)

    @Query("SELECT * FROM saved_match WHERE id = 1")
    fun observeSavedMatch(): Flow<SavedMatchEntity?>

    @Query("SELECT * FROM saved_match WHERE id = 1")
    suspend fun getSavedMatchOnce(): SavedMatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedMatch(match: SavedMatchEntity)

    @Query("DELETE FROM saved_match WHERE id = 1")
    suspend fun clearSavedMatch()

    @Query("SELECT * FROM daily_reward WHERE id = 1")
    fun observeDailyReward(): Flow<DailyRewardEntity?>

    @Query("SELECT * FROM daily_reward WHERE id = 1")
    suspend fun getDailyRewardOnce(): DailyRewardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyReward(reward: DailyRewardEntity)

    @Query("SELECT * FROM tournament_progress WHERE id = 1")
    fun observeTournament(): Flow<TournamentEntity?>

    @Query("SELECT * FROM tournament_progress WHERE id = 1")
    suspend fun getTournamentOnce(): TournamentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTournament(tournament: TournamentEntity)
}
