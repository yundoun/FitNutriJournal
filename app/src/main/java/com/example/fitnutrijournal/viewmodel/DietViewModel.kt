package com.example.fitnutrijournal.viewmodel

import com.example.fitnutrijournal.data.database.FoodDatabase // 여기에 import 추가
import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fitnutrijournal.data.model.DailyIntakeRecord
import com.example.fitnutrijournal.data.model.Food
import com.example.fitnutrijournal.data.model.Meal
import com.example.fitnutrijournal.data.model.MealWithFood
import com.example.fitnutrijournal.data.repository.DailyIntakeRecordRepository
import com.example.fitnutrijournal.data.repository.FoodRepository
import com.example.fitnutrijournal.data.repository.MealRepository
import kotlinx.coroutines.launch

class DietViewModel(application: Application, private val homeViewModel: HomeViewModel) : AndroidViewModel(application) {

    private val mealRepository: MealRepository
    private val foodRepository: FoodRepository
    private val dailyIntakeRecordRepository: DailyIntakeRecordRepository

    private val _dailyIntakeRecord = MutableLiveData<DailyIntakeRecord?>()

    private val _recentFoods = MediatorLiveData<List<Food>>()
    val recentFoods: LiveData<List<Food>> get() = _recentFoods

    init {
        val database = FoodDatabase.getDatabase(application)
        val dailyIntakeRecordDao = database.dailyIntakeRecordDao()
        val foodDao = database.foodDao()
        val mealDao = database.mealDao()

        dailyIntakeRecordRepository = DailyIntakeRecordRepository(dailyIntakeRecordDao)
        foodRepository = FoodRepository(foodDao)
        mealRepository = MealRepository(mealDao, foodDao)

        // 초기화 시 최근 음식 데이터를 로드
        loadRecentFoods()


    }

    private val _favorites = MutableLiveData<Set<String>>(emptySet())
    val favorites: LiveData<Set<String>> get() = _favorites

    val isFavorite = MutableLiveData<Boolean>()

    // 아이템 검색
    private val _searchQuery = MutableLiveData("")
    private val searchQuery: LiveData<String> get() = _searchQuery

    // 아이템 선택
    private val _selectedFood = MutableLiveData<Food>()
    val selectedFood: LiveData<Food> get() = _selectedFood
    fun selectFood(foodCode: String) {
        viewModelScope.launch {
            val food = foodRepository.getFoodByFoodCode(foodCode)
            _selectedFood.value = food

            // 기본 중량으로 설정하지 않고 _selectedMealQuantity 값이 있을 때만 업데이트
            if (_selectedMealQuantity.value == null) {
                updateNutrientValues(food.servingSize)
            }
        }
    }

    private val _totalContent = MutableLiveData<String>("")
    val totalContent: MutableLiveData<String> get() = _totalContent

    private val _calculatedCalories = MutableLiveData<String>("")
    val calculatedCalories: MutableLiveData<String> get() = _calculatedCalories

    private val _calculatedCarbohydrate = MutableLiveData<String>("")
    val calculatedCarbohydrate: MutableLiveData<String> get() = _calculatedCarbohydrate

    private val _calculatedProtein = MutableLiveData<String>("")
    val calculatedProtein: MutableLiveData<String> get() = _calculatedProtein

    private val _calculatedFat = MutableLiveData<String>("")
    val calculatedFat: MutableLiveData<String> get() = _calculatedFat



    private fun loadRecentFoods() {
        val recentMealsLiveData = mealRepository.getRecentMeals(20)
        _recentFoods.addSource(recentMealsLiveData) { meals ->
            viewModelScope.launch {
                val foods = meals.map { meal ->
                    foodRepository.getFoodByFoodCode(meal.dietFoodCode)
                }.distinctBy { it.foodCd }
                _recentFoods.postValue(foods)
            }
        }
    }

    // 버튼 가시성 설정

    private val _isCheckboxVisible = MutableLiveData<Boolean>(true)
    val isCheckboxVisible: LiveData<Boolean> get() = _isCheckboxVisible

    private val _isFavoriteButtonVisible = MutableLiveData<Boolean>(true)
    val isFavoriteButtonVisible: LiveData<Boolean> get() = _isFavoriteButtonVisible

    private val _isSaveButtonVisible = MutableLiveData<Boolean>(true)
    val isSaveButtonVisible: LiveData<Boolean> get() = _isSaveButtonVisible

    private val _isUpdateButtonVisible = MutableLiveData<Boolean>(true)
    val isUpdateButtonVisible: LiveData<Boolean> get() = _isUpdateButtonVisible

    private val _isAddFromLibraryButtonVisible = MutableLiveData<Boolean>(true)
    val isAddFromLibraryButtonVisible: LiveData<Boolean> get() = _isAddFromLibraryButtonVisible
    fun setAddFromLibraryButtonVisible(isVisible: Boolean) {
        _isAddFromLibraryButtonVisible.value = isVisible
    }

    private val _isLongClickEnabled = MutableLiveData<Boolean>(true)
    val isLongClickEnabled: LiveData<Boolean> get() = _isLongClickEnabled

    fun setLongClickEnabled(enabled: Boolean) {
        _isLongClickEnabled.value = enabled
    }



    // 체크된 아이템
    private val _checkedItems = MutableLiveData<Set<Food>>(emptySet())
    val checkedItems: LiveData<Set<Food>> get() = _checkedItems

    // 체크된 아이템 개수 카운트
    private val _selectedCountFoodItem = MutableLiveData<Int>(0)
    val selectedCountFoodItem: LiveData<Int> get() = _selectedCountFoodItem

    // 식사 타입에 따라 데이터 필터링
    private val _mealType = MutableLiveData<String>("")
    val mealType: LiveData<String> get() = _mealType

    // 식사 데이터 데이터베이스에서 가져와 저장
    private val _mealsWithFood = MutableLiveData<List<MealWithFood>>()

    private val _selectedMealQuantity = MutableLiveData<Int?>()
    val selectedMealQuantity: LiveData<Int?> get() = _selectedMealQuantity

    private suspend fun generateFoodCode(): String {
        val maxFoodCd = foodRepository.getMaxFoodCd()
        return if (maxFoodCd != null) {
            val numberPart = maxFoodCd.substring(1).toInt()
            Log.d("DietViewModel", "Number part: $numberPart")
            val newNumberPart = numberPart + 1
            "D" + newNumberPart.toString().padStart(5, '0')
        } else {
            "D00001"
        }
    }

    fun insertFood(food: Food) {
        viewModelScope.launch {
            val newFoodCd = generateFoodCode()
            val newFood = food.copy(foodCd = newFoodCd)
            foodRepository.insert(newFood)
            Log.d("DietViewModel", "New food inserted with code: $newFoodCd")
        }
    }

    fun setMealType(type: String) {
        _mealType.value = type
    }


    fun clearSelectedCountFoodItem() {
        _selectedCountFoodItem.value = 0
    }

    fun setCheckboxVisible(isVisible: Boolean) {
        _isCheckboxVisible.value = isVisible
    }

    fun setFavoriteButtonVisibility(isVisible: Boolean) {
        _isFavoriteButtonVisible.value = isVisible
    }

    fun setSaveButtonVisibility(isVisible: Boolean) {
        _isSaveButtonVisible.value = isVisible
    }

    fun setUpdateButtonVisibility(isVisible: Boolean) {
        _isUpdateButtonVisible.value = isVisible
    }

    fun setAddFromLibraryButtonVisibility(isVisible: Boolean) {
        _isAddFromLibraryButtonVisible.value = isVisible
    }

    // =================================  모든 Food 데이터 가져와 저장
    private val allFoods: LiveData<List<Food>> = foodRepository.allFoods

    // 정렬 기준 LiveData
    private val _sortOrder = MutableLiveData<SortOrder>(SortOrder.ASCENDING)
    private val sortOrder: LiveData<SortOrder> get() = _sortOrder

    enum class SortOrder {
        ASCENDING,
        DESCENDING
    }

    // 정렬 기준 설정 메서드
    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    // RecyclerView 바인딩 하기 위한 Food 목록 필터링
    val filteredFoods = MediatorLiveData<List<Food>>().apply {
        addSource(allFoods) { foods ->
            value = filterAndSortFoods(foods, searchQuery.value.orEmpty(), sortOrder.value ?: SortOrder.ASCENDING)
        }
        addSource(searchQuery) { query ->
            value = filterAndSortFoods(allFoods.value.orEmpty(), query, sortOrder.value ?: SortOrder.ASCENDING)
        }
        addSource(sortOrder) { order ->
            value = filterAndSortFoods(allFoods.value.orEmpty(), searchQuery.value.orEmpty(), order)
        }
    }

    val favoriteFoods = MediatorLiveData<List<Food>>().apply {
        addSource(allFoods) { foods ->
            value = filterAndSortFoods(foods.filter { it.isFavorite }, searchQuery.value.orEmpty(), sortOrder.value ?: SortOrder.ASCENDING)
        }
        addSource(searchQuery) { query ->
            value = filterAndSortFoods(allFoods.value.orEmpty().filter { it.isFavorite }, query, sortOrder.value ?: SortOrder.ASCENDING)
        }
        addSource(sortOrder) { order ->
            value = filterAndSortFoods(allFoods.value.orEmpty().filter { it.isFavorite }, searchQuery.value.orEmpty(), order)
        }
    }


    val userAddedFoods = MediatorLiveData<List<Food>>().apply {
        addSource(allFoods) { foods ->
            value = filterAndSortFoods(foods.filter { it.isAddedByUser }, searchQuery.value.orEmpty(), sortOrder.value ?: SortOrder.ASCENDING)
        }
        addSource(searchQuery) { query ->
            value = filterAndSortFoods(allFoods.value.orEmpty().filter { it.isAddedByUser }, query, sortOrder.value ?: SortOrder.ASCENDING)
        }
        addSource(sortOrder) { order ->
            value = filterAndSortFoods(allFoods.value.orEmpty().filter { it.isAddedByUser }, searchQuery.value.orEmpty(), order)
        }
    }

    private fun filterAndSortFoods(foods: List<Food>, query: String, sortOrder: SortOrder): List<Food> {
        val filtered = if (query.isEmpty()) {
            foods
        } else {
            foods.filter { it.foodName.contains(query, ignoreCase = true) }
        }
        return when (sortOrder) {
            SortOrder.ASCENDING -> filtered.sortedBy { it.foodName }
            SortOrder.DESCENDING -> filtered.sortedByDescending { it.foodName }
        }
    }

    init {
        // favoriteFoods LiveData를 관찰하여 favorite 목록 업데이트
        favoriteFoods.observeForever { favoriteList ->
            _favorites.value = favoriteList.map { it.foodCd }.toSet()
        }

        selectedFood.observeForever { food ->
            isFavorite.value = food?.isFavorite ?: false
        }
    }


    // 체크 상태 업데이트 메서드
    fun toggleCheckedItem(item: Food) {
        val currentCheckedItems = _checkedItems.value ?: emptySet()
        if (currentCheckedItems.contains(item)) {
            _checkedItems.value = currentCheckedItems - item
        } else {
            _checkedItems.value = currentCheckedItems + item
        }
        _checkedItems.value = _checkedItems.value // 트리거
        logCheckedItems()
    }

    // 체크된 아이템 초기화 메서드
    fun clearCheckedItems() {
        _checkedItems.value = emptySet()
        _checkedItems.value = _checkedItems.value // 트리거
       Log.d("DietViewModel", "Cleared checked items")
    }

    // 체크된 아이템 로그 출력 메서드
    private fun logCheckedItems() {
        val checkedItems = _checkedItems.value ?: return
        _selectedCountFoodItem.value = checkedItems.size
        Log.d("DietViewModel", "Checked items count: ${checkedItems.size}")
        checkedItems.forEach { item ->
            Log.d(
                "DietViewModel",
                "Checked item: ${item.foodCd}, serving size: ${item.servingSize}, "
            )
        }
    }

    fun updateTotalContent(value: String) {
        _totalContent.value = value
        val totalContent = value.toIntOrNull() ?: 0
        updateNutrientValues(totalContent)
    }

    // 중량에 맞게 영양성분 계산
    private fun updateNutrientValues(totalContent: Int) {
        val food = _selectedFood.value ?: return
        if (food.servingSize == 0) return

        Log.d("DietViewModel", "Calculating nutrients with totalContent: $totalContent and servingSize: ${food.servingSize}")

        val factor = totalContent.toFloat() / food.servingSize
        _calculatedCalories.value = String.format("%.2f", food.calories * factor)
        _calculatedCarbohydrate.value = String.format("%.2f", food.carbohydrate * factor)
        _calculatedProtein.value = String.format("%.2f", food.protein * factor)
        _calculatedFat.value = String.format("%.2f", food.fat * factor)

        Log.d("DietViewModel", "Calculated calories: ${_calculatedCalories.value}, carbs: ${_calculatedCarbohydrate.value}, protein: ${_calculatedProtein.value}, fat: ${_calculatedFat.value}")
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite() {
        selectedFood.value?.let { food ->
            viewModelScope.launch {
                val currentFavorites = _favorites.value ?: emptySet()
                if (currentFavorites.contains(food.foodCd)) {
                    _favorites.value = currentFavorites - food.foodCd
                    food.isFavorite = false
                } else {
                    _favorites.value = currentFavorites + food.foodCd
                    food.isFavorite = true
                }
                isFavorite.value = food.isFavorite
                foodRepository.update(food)
            }
        }
    }

    // 사용자가 설정 중량 저장
    @RequiresApi(Build.VERSION_CODES.O)
    fun saveCurrentFoodIntake() {
        viewModelScope.launch {
            val food = _selectedFood.value ?: return@launch
            val date = homeViewModel.currentDate.value ?: return@launch
            val mealType = _mealType.value ?: return@launch
            val totalContent = _totalContent.value?.toIntOrNull() ?: return@launch

            val meal = Meal(
                date = date,
                mealType = mealType,
                dietFoodCode = food.foodCd,
                quantity = totalContent
            )

            val calories = _calculatedCalories.value?.toFloatOrNull() ?: 0f
            val carbs = _calculatedCarbohydrate.value?.toFloatOrNull() ?: 0f
            val protein = _calculatedProtein.value?.toFloatOrNull() ?: 0f
            val fat = _calculatedFat.value?.toFloatOrNull() ?: 0f

            val initialRecord = dailyIntakeRecordRepository.getRecordByDate(date)
                ?: DailyIntakeRecord(date)

            val updatedRecord = initialRecord.copy(
                currentCalories = (initialRecord.currentCalories + calories).toInt(),
                currentCarbs = (initialRecord.currentCarbs + carbs).toInt(),
                currentProtein = (initialRecord.currentProtein + protein).toInt(),
                currentFat = (initialRecord.currentFat + fat).toInt()
            )

            // 식사 및 섭취 기록 저장
            mealRepository.insert(meal)
            dailyIntakeRecordRepository.insert(updatedRecord)
            _dailyIntakeRecord.postValue(updatedRecord)

            Log.d(
                "DietViewModel",
                "Saved meal: ${meal.dietFoodCode}, Date: ${meal.date}, Meal type: ${meal.mealType}, Quantity: ${meal.quantity}"
            )

            Log.d(
                "DietViewModel",
                "Updated record: ${updatedRecord.date}, Calories: ${updatedRecord.currentCalories}, Carbs: ${updatedRecord.currentCarbs}, Protein: ${updatedRecord.currentProtein}, Fat: ${updatedRecord.currentFat}"
            )

            homeViewModel.updateNutrientData(mealType, food, totalContent)
            homeViewModel.refreshFoodNames()  // 음식 추가 후 foodNames 업데이트
        }
    }


    // Food 데이터 삭제
    @RequiresApi(Build.VERSION_CODES.O)
    fun deleteFood(food: Food) {
        viewModelScope.launch {
            // 식사에 해당 음식이 포함되어 있는지 확인
            val mealsWithFood = mealRepository.getMealsByFoodCode(food.foodCd)
            mealsWithFood.forEach { meal ->
                val quantity = meal.quantity
                val mealType = meal.mealType
                Log.d("DietViewModel", "Deleting meal: $meal with food: $food")
                // 섭취량 감소
                homeViewModel.updateNutrientData(mealType, food, -quantity)
                // 식사 데이터 삭제
                mealRepository.deleteMealById(meal.id)
            }
            Log.d("DietViewModel", "Deleting food: $food")

            // 음식 데이터 삭제
            foodRepository.delete(food)
            // 섭취 기록 업데이트
            homeViewModel.refreshFilteredFoods()
            homeViewModel.refreshFoodNames() // 음식 삭제 후 foodNames 업데이트
        }
    }

    fun setSelectedMealQuantity(quantity: Int) {
        _selectedMealQuantity.value = quantity
    }

    fun clearSelectedMealQuantity() {
        _selectedMealQuantity.value = null
    }

    fun loadMealsWithFood() {
        viewModelScope.launch {
            val mealWithFoodList = mealRepository.getAllMealsWithFood()
            _mealsWithFood.postValue(mealWithFoodList)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun deleteMealById(mealId: Long) {
        viewModelScope.launch {
            val mealWithFood = mealRepository.getMealWithFoodById(mealId)
            if (mealWithFood != null) {
                val quantity = mealWithFood.meal.quantity
                val mealType = mealWithFood.meal.mealType
                Log.d("DietViewModel", "Deleting meal: ${mealWithFood.meal}")
                // 섭취량 감소
                homeViewModel.updateNutrientData(mealType, mealWithFood.food, -quantity)
                // 식사 데이터 삭제
                mealRepository.deleteMealById(mealId)
                Log.d("DietViewModel", "Deleted meal with id: $mealId")

                // 섭취 기록 업데이트
                homeViewModel.refreshFilteredFoods()
                homeViewModel.refreshFoodNames() //foodNames 업데이트
            } else {
                Log.d("DietViewModel", "Meal not found with id: $mealId")
            }
        }
    }

    // 실행 취소 시 음식 복구
    @RequiresApi(Build.VERSION_CODES.O)
    fun addMealWithFood(mealWithFood: MealWithFood) {
        viewModelScope.launch {
            val quantity = mealWithFood.meal.quantity
            val mealType = mealWithFood.meal.mealType
            Log.d("DietViewModel", "Adding meal: ${mealWithFood.meal}")
            // 섭취량 증가
            homeViewModel.updateNutrientData(mealType, mealWithFood.food, quantity)
            // 식사 데이터 추가
            mealRepository.insert(mealWithFood.meal)
            Log.d("DietViewModel", "Added meal with id: ${mealWithFood.meal.id}")

            // 섭취 기록 업데이트
            homeViewModel.refreshFilteredFoods()
            homeViewModel.refreshFoodNames() // foodNames 업데이트
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateFoodIntake(mealId: Long) {
        val food = _selectedFood.value ?: return
        val newQuantity = _totalContent.value?.toIntOrNull() ?: return
        viewModelScope.launch {
            val mealType = _mealType.value ?: return@launch

            val mealWithFood = mealRepository.getMealWithFoodById(mealId)
            mealWithFood?.let {
                val oldQuantity = it.meal.quantity

                val updatedMeal = it.meal.copy(quantity = newQuantity)
                Log.d("DietViewModel", "Updating meal: $updatedMeal")
                mealRepository.update(updatedMeal)

                // 기존 섭취량을 제거하고, 새로운 섭취량을 추가하여 영양성분 업데이트
                homeViewModel.updateNutrientData(mealType, food, -oldQuantity)
                homeViewModel.updateNutrientData(mealType, food, newQuantity)

                homeViewModel.refreshFilteredFoods()
                homeViewModel.refreshFoodNames() //foodNames 업데이트
            } ?: run {
                Log.d("DietViewModel", "Meal not found with id: $mealId")
            }
        }
    }
}
