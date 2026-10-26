package app.honours.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.room.Upsert
import app.honours.domain.Award
import app.honours.domain.Given
import app.honours.domain.Own
import app.honours.domain.Pin
import app.honours.domain.Shape

class Converters {
    @TypeConverter fun shape(s: Shape): String = s.name
    @TypeConverter fun shape(s: String): Shape = Shape.entries.firstOrNull { it.name == s } ?: Shape.ROUND
}

@Dao
interface HonoursDao {
    @Query("SELECT * FROM award ORDER BY at, id") suspend fun awards(): List<Award>
    @Query("SELECT * FROM pin ORDER BY pennant, at") suspend fun pins(): List<Pin>
    @Query("SELECT * FROM own ORDER BY id") suspend fun own(): List<Own>
    @Query("SELECT * FROM given ORDER BY at") suspend fun given(): List<Given>

    @Insert suspend fun insert(a: Award): Long
    @Upsert suspend fun pin(p: Pin)
    @Insert suspend fun insert(o: Own): Long
    @Update suspend fun update(o: Own)
    @Insert suspend fun insert(g: Given): Long
    @Query("DELETE FROM award WHERE id = :id") suspend fun deleteAward(id: Long)
    @Query("DELETE FROM pin WHERE pennant = :pennant AND `key` = :key") suspend fun unpin(pennant: Int, key: String)
}

@Database(entities = [Award::class, Pin::class, Own::class, Given::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class HonoursDb : RoomDatabase() {
    abstract fun dao(): HonoursDao

    companion object {
        fun create(c: Context): HonoursDb = Room.databaseBuilder(c, HonoursDb::class.java, "honours.db").build()
        fun memory(c: Context): HonoursDb = Room.inMemoryDatabaseBuilder(c, HonoursDb::class.java).build()
    }
}
