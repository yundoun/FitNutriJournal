package com.example.fitnutrijournal.data.repository

import androidx.lifecycle.LiveData
import com.example.fitnutrijournal.data.dao.MealDao
import com.example.fitnutrijournal.data.model.Meal
import com.example.fitnutrijournal.data.model.MealWithFood

class MealRepository(private val mealDao: MealDao) {

    // 주어진 Meal 객체를 삽입하는 역할
    suspend fun insert(meal: Meal) {
        mealDao.insert(meal)
    }

    suspend fun update(meal: Meal) {
        mealDao.update(meal)
    }

    // 특정 날짜(date)에 해당하는 Meal 객체 리스트를 LiveData 형태로 반환
    fun getMealsByDate(date: String): LiveData<List<Meal>> {
        return mealDao.getMealsByDate(date)
    }

    // 특정 날짜(date)에 해당하는 Meal 객체 리스트를 반환
    suspend fun getMealsByDateAndTypeSync(date: String, mealType: String): List<Meal> {
        return mealDao.getMealsByDateAndTypeSync(date, mealType)
    }

    suspend fun getMealByFoodCodeAndDate(foodCode: String, date: String): Meal? {
        return mealDao.getMealByFoodCodeAndDate(foodCode, date)
    }
    suspend fun deleteMealById(id: Long) {
        mealDao.deleteMealById(id)
    }

    // 특정 식품 코드에 해당하는 Meal 객체 리스트를 반환
    suspend fun getMealsByFoodCode(foodCode: String): List<Meal> {
        return mealDao.getMealsByFoodCode(foodCode)
    }


    suspend fun getMealWithFoodById(mealId: Long): MealWithFood? {
        return mealDao.getMealWithFoodById(mealId)
    }

    suspend fun getAllMealsWithFood(): List<MealWithFood> {
        return mealDao.getAllMealsWithFood()
    }

}
