package com.example.fitnutrijournal.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fitnutrijournal.data.model.Food

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: Food)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<Food>)

    @Update
    suspend fun update(food: Food)

    @Query("SELECT * FROM Food WHERE foodCd = :foodCode")
    suspend fun getFoodByFoodCode(foodCode: String): Food

    // 모든 Food 객체 리스트를 LiveData 형태로 반환
    @Query("SELECT * FROM Food")
    fun getAllFoods(): LiveData<List<Food>>

    // 즐겨찾기(isFavorite)로 설정된 Food 객체 리스트를 LiveData 형태로 반환
    @Query("SELECT * FROM Food WHERE isFavorite = 1")
    fun getFavoriteFoods(): LiveData<List<Food>>

    // 사용자가 추가한(isAddedByUser) Food 객체 리스트를 LiveData 형태로 반환
    @Query("SELECT * FROM Food WHERE isAddedByUser = 1")
    fun getUserAddedFoods(): LiveData<List<Food>>

    // 모든 Food 객체 리스트를 반환
    @Query("SELECT * FROM Food")
    suspend fun getAllFoodsList(): List<Food>
}
