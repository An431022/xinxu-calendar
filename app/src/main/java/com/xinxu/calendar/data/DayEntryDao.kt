package com.xinxu.calendar.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DayEntryDao {

    /** 启动时一次读空。一天最多一条，十年也才三千多行，全量读进内存比反复查询更简单也更快。 */
    @Query("SELECT * FROM day_entry")
    suspend fun all(): List<DayEntryEntity>

    /**
     * 某个月的全部记录。
     *
     * `:prefix` 传 `"2026-10"`。用 `LIKE :prefix || '%'` 而不是 `BETWEEN`，
     * 是因为 ISO 日期的字符串前缀天然就是"这个月"，不用算月末是哪天，
     * 也就不会在闰年、月末那几天上出错。
     */
    @Query("SELECT * FROM day_entry WHERE date LIKE :prefix || '%' ORDER BY date")
    suspend fun ofMonth(prefix: String): List<DayEntryEntity>

    /** 跨月区间，给 v0.3 的年度马赛克视图用。 */
    @Query("SELECT * FROM day_entry WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun between(from: String, to: String): List<DayEntryEntity>

    /**
     * 写入或覆盖。用 `@Upsert` 而不是 `@Insert(onConflict = REPLACE)`：
     * 后者在遇到冲突时是"删掉旧的再插"，外键级联时的行为和 upsert 不同，语义容易出错。
     */
    @Upsert
    suspend fun upsert(entity: DayEntryEntity)

    /**
     * 删掉一整天的记录。
     *
     * 现在没人调用 —— 取消涂色是"再点一次那个颜色"，取消到最后一天变空，
     * 但**空记录留着不删**：留白本身也是一种状态（设计红线 2），
     * 而且将来加了备注之后，删掉行就等于把备注一起删了。
     */
    @Query("DELETE FROM day_entry WHERE date = :date")
    suspend fun delete(date: String)
}
