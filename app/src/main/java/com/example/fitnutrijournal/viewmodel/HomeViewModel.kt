package com.example.fitnutrijournal.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.fitnutrijournal.data.database.FoodDatabase
import com.example.fitnutrijournal.data.model.DailyIntakeGoal
import com.example.fitnutrijournal.data.model.DailyIntakeRecord
import com.example.fitnutrijournal.data.model.Food
import com.example.fitnutrijournal.data.model.Meal
import com.example.fitnutrijournal.data.model.Memo
import com.example.fitnutrijournal.data.repository.DailyIntakeGoalRepository
import com.example.fitnutrijournal.data.repository.DailyIntakeRecordRepository
import com.example.fitnutrijournal.data.repository.FoodRepository
import com.example.fitnutrijournal.data.repository.MealRepository
import com.example.fitnutrijournal.data.repository.MemoRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter


@RequiresApi(Build.VERSION_CODES.O)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val foodRepository: FoodRepository
    val mealRepository: MealRepository
    private val dailyIntakeGoalRepository: DailyIntakeGoalRepository
    private val dailyIntakeRecordRepository: DailyIntakeRecordRepository
    private val memoRepository: MemoRepository

    // 날짜
    private val _todayDate = MutableLiveData<String>().apply {
        value = LocalDate.now().format(dateFormatter)
    }
    private val todayDate: LiveData<String>
        get() = _todayDate

    // 현재 날짜 = 오늘 날짜 기본값
    private val _currentDate = MutableLiveData<String>().apply {
        value = todayDate.value
    }
    val currentDate: LiveData<String>
        get() = _currentDate

    private val _selectedDate = MutableLiveData<String>()
    val selectedDate: LiveData<String>
        get() = _selectedDate

    private fun setCurrentDate(date: String) {
        if (_currentDate.value != date) {
            _currentDate.value = date
            Log.d("HomeViewModel", "Current date set to: $date")
        }
    }

    // ============================== 식사 목록 텍스트==============================
    private val _breakfastFoodNames = MutableLiveData<List<String>>()
    val breakfastFoodNames: LiveData<String> = _breakfastFoodNames.map { foods ->
        foods.joinToString(", ")
    }

    private val _lunchFoodNames = MutableLiveData<List<String>>()
    val lunchFoodNames: LiveData<String> = _lunchFoodNames.map { foods ->
        foods.joinToString(", ")
    }

    private val _dinnerFoodNames = MutableLiveData<List<String>>()
    val dinnerFoodNames: LiveData<String> = _dinnerFoodNames.map { foods ->
        foods.joinToString(", ")
    }

    private val _snackFoodNames = MutableLiveData<List<String>>()
    val snackFoodNames: LiveData<String> = _snackFoodNames.map { foods ->
        foods.joinToString(", ")
    }



    // ============================== Database에서 가져온 데이터 ==============================
    // 하루 섭취 목표 ( 날짜, 총 칼로리, 아침, 점심, 저녁, 간식 칼로리 )
    private val _todayGoal = MutableLiveData<DailyIntakeGoal?>() // 할당 완료
    val todayGoal: LiveData<DailyIntakeGoal?> get() = _todayGoal

    // 하루 섭취 기록 ( 날짜, 현재 칼로리, 탄수화물, 단백질, 지방 )
    private val _dailyIntakeRecord = MutableLiveData<DailyIntakeRecord?>()
    val dailyIntakeRecord: LiveData<DailyIntakeRecord?> get() = _dailyIntakeRecord

    // 각 식사 유형별로 섭취한 영양소 데이터를 저장할 변수들 (칼로리, 탄수화물, 단백질, 지방, 수량)
    private val _breakfastNutrients = MutableLiveData<NutrientData>()
    val breakfastNutrients: LiveData<NutrientData> get() = _breakfastNutrients

    private val _lunchNutrients = MutableLiveData<NutrientData>()
    val lunchNutrients: LiveData<NutrientData> get() = _lunchNutrients

    private val _dinnerNutrients = MutableLiveData<NutrientData>()
    val dinnerNutrients: LiveData<NutrientData> get() = _dinnerNutrients

    private val _snackNutrients = MutableLiveData<NutrientData>()
    val snackNutrients: LiveData<NutrientData> get() = _snackNutrients

    // ========================= 목표 섭취량 데이터 =========================

    // 매크로 비율 설정
    private val carbRatio = 0.5
    private val proteinRatio = 0.3
    private val fatRatio = 0.2

    // 매크로 목표 계산 함수
    private fun calculateMacros(calories: Int): Triple<Int, Int, Int> {
        val carbs = (calories * carbRatio / 4).toInt()
        val protein = (calories * proteinRatio / 4).toInt()
        val fat = (calories * fatRatio / 9).toInt()
        return Triple(carbs, protein, fat)
    }

    // 오늘 하루 목표 섭취량
    private val _targetCalories = MediatorLiveData<Int>().apply {
        addSource(_todayGoal) { goal ->
            value = goal?.targetCalories ?: 0
        }
    }
    private val _targetCarbIntake = MediatorLiveData<Int>().apply {
        value = 0 // 초기값 0
        addSource(_targetCalories) { calories ->
            val (carbs, _, _) = calculateMacros(calories)
            value = carbs
            Log.d("progressbar", "Carb Intake updated: $carbs")
        }
    }
    private val _targetProteinIntake = MediatorLiveData<Int>().apply {
        value = 0
        addSource(_targetCalories) { calories ->
            val (_, protein, _) = calculateMacros(calories)
            value = protein
            Log.d("progressbar", "Protein Intake updated: $protein")
        }
    }
    private val _targetFatIntake = MediatorLiveData<Int>().apply {
        value = 0
        addSource(_targetCalories) { calories ->
            val (_, _, fat) = calculateMacros(calories)
            value = fat
            Log.d("progressbar", "Fat Intake updated: $fat")
        }
    }

    val targetCalories: LiveData<Int>
        get() = _targetCalories
    val targetCarbIntake: LiveData<Int>
        get() = _targetCarbIntake
    val targetProteinIntake: LiveData<Int>
        get() = _targetProteinIntake
    val targetFatIntake: LiveData<Int>
        get() = _targetFatIntake


    // 아침 목표 섭취량
    private val _targetCaloriesBreakfast = MediatorLiveData<Int>().apply {
        addSource(_todayGoal) { goal ->
            value = goal?.targetBreakfast ?: 0
        }
    }
    private val _targetCarbIntakeBreakfast = MediatorLiveData<Int>().apply {
        addSource(_targetCaloriesBreakfast) { calories ->
            val (carbs, _, _) = calculateMacros(calories)
            value = carbs
        }
    }
    private val _targetProteinIntakeBreakfast = MediatorLiveData<Int>().apply {
        addSource(_targetCaloriesBreakfast) { calories ->
            val (_, protein, _) = calculateMacros(calories)
            value = protein
        }
    }
    private val _targetFatIntakeBreakfast = MediatorLiveData<Int>().apply {
        addSource(_targetCaloriesBreakfast) { calories ->
            val (_, _, fat) = calculateMacros(calories)
            value = fat
        }
    }
    val targetCaloriesBreakfast: LiveData<Int>
        get() = _targetCaloriesBreakfast
    val targetCarbIntakeBreakfast: LiveData<Int>
        get() = _targetCarbIntakeBreakfast
    val targetProteinIntakeBreakfast: LiveData<Int>
        get() = _targetProteinIntakeBreakfast
    val targetFatIntakeBreakfast: LiveData<Int>
        get() = _targetFatIntakeBreakfast

    // 점심 목표 섭취량
    private val _targetCaloriesLunch = MediatorLiveData<Int>().apply {
        addSource(_todayGoal) { goal ->
            value = goal?.targetLunch ?: 0
        }
    }
    private val _targetCarbIntakeLunch = MediatorLiveData<Int>().apply {
        addSource(_targetCaloriesLunch) { calories ->
            val (carbs, _, _) = calculateMacros(calories)
            value = carbs
        }
    }
    private val _targetProteinIntakeLunch = MediatorLiveData<Int>().apply {
        addSource(_targetCaloriesLunch) { calories ->
            val (_, protein, _) = calculateMacros(calories)
            value = protein
        }
    }
    private val _targetFatIntakeLunch = MediatorLiveData<Int>().apply {
        addSource(_targetCaloriesLunch) { calories ->
            val (_, _, fat) = calculateMacros(calories)
            value = fat
        }
    }
    val targetCaloriesLunch: LiveData<Int>
        get() = _targetCaloriesLunch
    val targetCarbIntakeLunch: LiveData<Int>
        get() = _targetCarbIntakeLunch
    val targetProteinIntakeLunch: LiveData<Int>
        get() = _targetProteinIntakeLunch
    val targetFatIntakeLunch: LiveData<Int>
        get() = _targetFatIntakeLunch

    // 저녁 목표 섭취량
    private val _targetCaloriesDinner = MediatorLiveData<Int>().apply {
        addSource(_todayGoal) { goal ->
            value = goal?.targetDinner ?: 0
        }
    }
    private val _targetCarbIntakeDinner = MediatorLiveData<Int>().apply {
        addSource(_targetCaloriesDinner) { calories ->
            val (carbs, _, _) = calculateMacros(calories)
            value = carbs
        }
    }
    private val _targetProteinIntakeDinner = MediatorLiveData<Int>().apply {
        addSource(_targetCaloriesDinner) { calories ->
            val (_, protein, _) = calculateMacros(calories)
            value = protein
        }
    }
    private val _targetFatIntakeDinner = MediatorLiveData<Int>().apply {
        addSource(_targetCaloriesDinner) { calories ->
            val (_, _, fat) = calculateMacros(calories)
            value = fat
        }
    }
    val targetCaloriesDinner: LiveData<Int>
        get() = _targetCaloriesDinner
    val targetCarbIntakeDinner: LiveData<Int>
        get() = _targetCarbIntakeDinner
    val targetProteinIntakeDinner: LiveData<Int>
        get() = _targetProteinIntakeDinner
    val targetFatIntakeDinner: LiveData<Int>
        get() = _targetFatIntakeDinner

    // 간식 목표 섭취량
    private val _targetCaloriesSnack = MediatorLiveData<Int>().apply {
        addSource(_todayGoal) { goal ->
            value = goal?.targetSnack ?: 0
        }
    }
    private val _targetCarbIntakeSnack = MediatorLiveData<Int>().apply {
        addSource(_targetCaloriesSnack) { calories ->
            val (carbs, _, _) = calculateMacros(calories)
            value = carbs
        }
    }
    private val _targetProteinIntakeSnack = MediatorLiveData<Int>().apply {
        addSource(_targetCaloriesSnack) { calories ->
            val (_, protein, _) = calculateMacros(calories)
            value = protein
        }
    }
    private val _targetFatIntakeSnack = MediatorLiveData<Int>().apply {
        addSource(_targetCaloriesSnack) { calories ->
            val (_, _, fat) = calculateMacros(calories)
            value = fat
        }
    }
    val targetCaloriesSnack: LiveData<Int>
        get() = _targetCaloriesSnack
    val targetCarbIntakeSnack: LiveData<Int>
        get() = _targetCarbIntakeSnack
    val targetProteinIntakeSnack: LiveData<Int>
        get() = _targetProteinIntakeSnack
    val targetFatIntakeSnack: LiveData<Int>
        get() = _targetFatIntakeSnack

    // ====================================================================


    // ================================== 섭취한 영양소 데이터 ==================================

    // 현재 섭취한 모든 영양소의 합 (칼로리, 탄수화물, 단백질, 지방)
    private val _currentTotalCalories = MediatorLiveData<Int>().apply {
        value = _dailyIntakeRecord.value?.currentCalories ?: 0
        addSource(_dailyIntakeRecord) { record ->
            value = record?.currentCalories ?: 0
        }
    }
    private val _currentCarbIntake = MediatorLiveData<Int>().apply {
        addSource(_dailyIntakeRecord) { record ->
            value = record?.currentCarbs ?: 0
        }
    }
    private val _currentProteinIntake = MediatorLiveData<Int>().apply {
        addSource(_dailyIntakeRecord) { record ->
            value = record?.currentProtein ?: 0
        }
    }
    private val _currentFatIntake = MediatorLiveData<Int>().apply {
        addSource(_dailyIntakeRecord) { record ->
            value = record?.currentFat ?: 0
        }
    }
    private val _remainingCalories = MediatorLiveData<Int>()
    val currentTotalCalories: LiveData<Int>
        get() = _currentTotalCalories
    val currentCarbIntake: LiveData<Int>
        get() =_currentCarbIntake
    val currentProteinIntake: LiveData<Int>
        get() =_currentProteinIntake
    val currentFatIntake: LiveData<Int>
        get() = _currentFatIntake
    val remainingCalories: LiveData<Int>
        get() = _remainingCalories

    // 섭취한 영양소 : 아침
    private val _currentCaloriesBreakfast = MediatorLiveData<Int>().apply {
        addSource(_breakfastNutrients) { nutrients ->
            value = nutrients.calories
        }
    }
    private val _currentCarbIntakeBreakfast = MediatorLiveData<Int>().apply {
        addSource(_breakfastNutrients) { nutrients ->
            value = nutrients.carbs.toInt()
        }
    }
    private val _currentProteinIntakeBreakfast = MediatorLiveData<Int>().apply {
        addSource(_breakfastNutrients) { nutrients ->
            value = nutrients.protein.toInt()
        }
    }
    private val _currentFatIntakeBreakfast = MediatorLiveData<Int>().apply {
        addSource(_breakfastNutrients) { nutrients ->
            value = nutrients.fat.toInt()
        }
    }

    val currentCaloriesBreakfast: LiveData<Int>
        get() = _currentCaloriesBreakfast
    val currentCarbIntakeBreakfast: LiveData<Int>
        get() = _currentCarbIntakeBreakfast
    val currentProteinIntakeBreakfast: LiveData<Int>
        get() = _currentProteinIntakeBreakfast
    val currentFatIntakeBreakfast: LiveData<Int>
        get() = _currentFatIntakeBreakfast

    // 섭취한 영양소 : 점심
    private val _currentCarbIntakeLunch = MediatorLiveData<Int>().apply {
        addSource(_lunchNutrients) { nutrients ->
            value = nutrients.carbs.toInt()
        }
    }
    private val _currentProteinIntakeLunch = MediatorLiveData<Int>().apply {
        addSource(_lunchNutrients) { nutrients ->
            value = nutrients.protein.toInt()
        }
    }
    private val _currentFatIntakeLunch = MediatorLiveData<Int>().apply {
        addSource(_lunchNutrients) { nutrients ->
            value = nutrients.fat.toInt()
        }
    }
    private val _currentCaloriesLunch = MediatorLiveData<Int>().apply {
        addSource(_lunchNutrients) { nutrients ->
            value = nutrients.calories
        }
    }

    val currentCaloriesLunch: LiveData<Int>
        get() = _currentCaloriesLunch
    val currentCarbIntakeLunch: LiveData<Int>
        get() = _currentCarbIntakeLunch
    val currentProteinIntakeLunch: LiveData<Int>
        get() = _currentProteinIntakeLunch
    val currentFatIntakeLunch: LiveData<Int>
        get() = _currentFatIntakeLunch

    // 섭취한 영양소 : 저녁
    private val _currentCarbIntakeDinner = MediatorLiveData<Int>().apply {
        addSource(_dinnerNutrients) { nutrients ->
            value = nutrients.carbs.toInt()
        }
    }
    private val _currentProteinIntakeDinner = MediatorLiveData<Int>().apply {
        addSource(_dinnerNutrients) { nutrients ->
            value = nutrients.protein.toInt()
        }
    }
    private val _currentFatIntakeDinner = MediatorLiveData<Int>().apply {
        addSource(_dinnerNutrients) { nutrients ->
            value = nutrients.fat.toInt()
        }
    }
    private val _currentCaloriesDinner = MediatorLiveData<Int>().apply {
        addSource(_dinnerNutrients) { nutrients ->
            value = nutrients.calories
        }
    }

    val currentCaloriesDinner: LiveData<Int>
        get() = _currentCaloriesDinner
    val currentCarbIntakeDinner: LiveData<Int>
        get() = _currentCarbIntakeDinner
    val currentProteinIntakeDinner: LiveData<Int>
        get() = _currentProteinIntakeDinner
    val currentFatIntakeDinner: LiveData<Int>
        get() = _currentFatIntakeDinner

    // 섭취한 영양소 : 간식
    private val _currentCaloriesSnack = MediatorLiveData<Int>().apply {
        addSource(_snackNutrients) { nutrients ->
            value = nutrients.calories
        }
    }
    private val _currentCarbIntakeSnack = MediatorLiveData<Int>().apply {
        addSource(_snackNutrients) { nutrients ->
            value = nutrients.carbs.toInt()
        }
    }
    private val _currentProteinIntakeSnack = MediatorLiveData<Int>().apply {
        addSource(_snackNutrients) { nutrients ->
            value = nutrients.protein.toInt()
        }
    }
    private val _currentFatIntakeSnack = MediatorLiveData<Int>().apply {
        addSource(_snackNutrients) { nutrients ->
            value = nutrients.fat.toInt()
        }
    }

    val currentCaloriesSnack: LiveData<Int>
        get() = _currentCaloriesSnack
    val currentCarbIntakeSnack: LiveData<Int>
        get() = _currentCarbIntakeSnack
    val currentProteinIntakeSnack: LiveData<Int>
        get() = _currentProteinIntakeSnack
    val currentFatIntakeSnack: LiveData<Int>
        get() = _currentFatIntakeSnack

    // 식단 자세히 보기
    private val _mealType = MutableLiveData<String>()
    val mealType: LiveData<String> get() = _mealType

    fun setMealType(type: String) {
        _mealType.value = type
    }

    private val _filteredFoods = MutableLiveData<List<Food>>()
    val filteredFoods: LiveData<List<Food>> get() = _filteredFoods

    // 필터링된 음식을 텍스트로 변환
    val filteredFoodsText = MediatorLiveData<String>().apply {
        addSource(_filteredFoods) { foods ->
            value = foods.joinToString(", ") { it.foodName }
        }
    }


    // 식단 자세히 보기
     fun filterFoodsByMealType(mealType: String) {
        viewModelScope.launch {
            val date = currentDate.value ?: LocalDate.now().format(dateFormatter)
            Log.d("HomeViewModel", "Filtering foods for date: $date and meal type: $mealType")
            val meals = mealRepository.getMealsByDateAndTypeSync(date, mealType)
            Log.d("HomeViewModel", "Meals: $meals")
            val foods = mutableListOf<Food>()

            for (meal in meals) {
                val food = foodRepository.getFoodByFoodCode(meal.dietFoodCode)
                foods.add(food)
            }

            _filteredFoods.postValue(foods)
            Log.d("HomeViewModel", "Filtered foods: ${foods.map { it.foodName }}")
        }
    }

    // ====================================================================




    private val _monthlyIntakeRecords = MutableLiveData<Map<LocalDate, DailyIntakeRecord>>()
    val monthlyIntakeRecords: LiveData<Map<LocalDate, DailyIntakeRecord>> get() = _monthlyIntakeRecords




    init {
        val database = FoodDatabase.getDatabase(application)
        val dailyIntakeGoalDao = database.dailyIntakeGoalDao()
        val dailyIntakeRecordDao = database.dailyIntakeRecordDao()
        val foodDao = database.foodDao()
        val mealDao = database.mealDao()
        val memoDao = FoodDatabase.getDatabase(application).memoDao()

        memoRepository = MemoRepository(memoDao)
        dailyIntakeGoalRepository = DailyIntakeGoalRepository(dailyIntakeGoalDao)
        dailyIntakeRecordRepository = DailyIntakeRecordRepository(dailyIntakeRecordDao)
        foodRepository = FoodRepository(foodDao)
        mealRepository = MealRepository(mealDao, foodDao)

        // 현재 날짜의 섭취 목표를 로드합니다.
        loadDailyIntakeGoal(selectedDate.value ?: LocalDate.now().format(dateFormatter))


        // 데이터 로드
        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        setCurrentDate(currentDate)
        loadDailyIntakeForDate(currentDate)

        // 식단 상세보기
        _mealType.observeForever { mealType ->
            filterFoodsByMealType(mealType)
        }

        // 앱 시작 시 오늘 날짜의 dailyIntakeRecord 값을 불러와 초기화하고 바인딩
        viewModelScope.launch {
            initializeDailyIntake(todayDate = currentDate)
        }

        // 잔여 칼로리 계산
        _remainingCalories.addSource(_currentTotalCalories) { updateRemainingCalories() }
        _remainingCalories.addSource(_targetCalories) { updateRemainingCalories() }

    }

    // 달력에 식사 섭취 데이터를 표시하기 위한 메소드
    fun loadIntakeRecordsForMonth(month: YearMonth) {
        viewModelScope.launch {
            val startDate = month.atDay(1)
            val endDate = month.atEndOfMonth()
            val records = dailyIntakeRecordRepository.getRecordsBetweenDates(startDate.toString(), endDate.toString())
            _monthlyIntakeRecords.value = records.associateBy { LocalDate.parse(it.date, dateFormatter) }
        }
    }


    // checkedItems를 Meal 객체로 변환
    fun addCheckedItemsToDailyIntakeRecord(checkedItems: Set<Food>, date: String, mealType: String) {
        viewModelScope.launch {
            Log.d("HomeViewModel", "Adding checked items: ${checkedItems.map { it.foodName }} to mealType: $mealType on date: $date")
            val meals = checkedItems.map { food ->
                Meal(
                    date = date,
                    mealType = mealType,
                    dietFoodCode = food.foodCd,
                    quantity = food.servingSize
                )
            }

            addMealsAndUpdateIntakeRecord(meals, mealType)
            refreshFilteredFoods()
            refreshFoodNames() //foodNames 업데이트
        }
    }

    // Meal 객체를 이용해서 DailyIntakeRecord와 각 식사 유형별 섭취한 영양소 데이터를 업데이트
    // Meal 객체를 데이터베이스에 삽입하고 해당 음식의 영양 성분을 누적함
    private suspend fun addMealsAndUpdateIntakeRecord(meals: List<Meal>, mealType: String) {
        // 첫 번째 meal의 날짜를 사용하여 DailyIntakeRecord를 가져옵니다.
        val date = meals.first().date
        val initialRecord = dailyIntakeRecordRepository.getRecordByDate(date)
            ?: DailyIntakeRecord(date)

        // 초기 값을 가져와서 누적합니다.
        var totalCalories = initialRecord.currentCalories
        var totalCarbs = initialRecord.currentCarbs
        var totalProtein = initialRecord.currentProtein
        var totalFat = initialRecord.currentFat

        for (meal in meals) {
            // Meal 데이터를 추가
            mealRepository.insert(meal)
            Log.d("HomeViewModel", "Inserted meal: $meal")

            // Food 데이터를 조회
            val food = foodRepository.getFoodByFoodCode(meal.dietFoodCode)
            Log.d("HomeViewModel", "Food found: $food for meal: $meal")


            // Food의 영양성분을 이용해 값을 누적합니다.
            totalCalories += (food.calories * meal.quantity / food.servingSize).toInt()
            totalCarbs += (food.carbohydrate * meal.quantity / food.servingSize).toInt()
            totalProtein += (food.protein * meal.quantity / food.servingSize).toInt()
            totalFat += (food.fat * meal.quantity / food.servingSize).toInt()

            // 각 식사 유형별로 섭취한 영양소 데이터 업데이트
            updateNutrientData(mealType, food, meal.quantity)
        }

        // 누적된 값을 사용하여 DailyIntakeRecord 업데이트
        val updatedRecord = initialRecord.copy(
            currentCalories = totalCalories,
            currentCarbs = totalCarbs,
            currentProtein = totalProtein,
            currentFat = totalFat
        )

        Log.d("HomeViewModel", "Updated record: $updatedRecord")

        // DailyIntakeRecord 업데이트
        dailyIntakeRecordRepository.insert(updatedRecord)
        _dailyIntakeRecord.postValue(updatedRecord)

        // 총 섭취 칼로리 업데이트
        _currentTotalCalories.postValue(totalCalories)
    }

    // 현재 날짜에 대한 식사 기록을 불러오는 메소드
    private fun loadDailyIntakeForDate(date: String) {
        viewModelScope.launch {
            val meals = mealRepository.getMealsByDate(date).value ?: return@launch
            val totalCalories = 0
            meals.forEach { meal ->
                val food = foodRepository.getFoodByFoodCode(meal.dietFoodCode)
                updateNutrientData(meal.mealType, food, meal.quantity)
            }
            _currentTotalCalories.postValue(totalCalories)
        }
    }


    // 특정 날짜(todayDate)의 일일 영양 섭취 기록을 초기화
    private suspend fun initializeDailyIntake(todayDate: String) {
        val record = dailyIntakeRecordRepository.getRecordByDate(todayDate)
        _dailyIntakeRecord.value = record
        _currentTotalCalories.value = record?.currentCalories ?: 0

        initializeMealNutrients(todayDate, "breakfast")
        initializeMealNutrients(todayDate, "lunch")
        initializeMealNutrients(todayDate, "dinner")
        initializeMealNutrients(todayDate, "snack")

        // 음식 이름 업데이트
        refreshFoodNames()
    }

    // 특정 날짜(date)와 식사 유형(mealType)에 대한 영양소 정보를 초기화
    private suspend fun initializeMealNutrients(date: String, mealType: String) {
        val meals = mealRepository.getMealsByDateAndTypeSync(date, mealType)
        var totalCalories = 0
        var totalCarbs = 0f
        var totalProtein = 0f
        var totalFat = 0f

        val foods = mutableListOf<String>()

        for (meal in meals) {
            val food = foodRepository.getFoodByFoodCode(meal.dietFoodCode)
            val foodDetails = "${food.foodName} (Calories: ${(food.calories * meal.quantity / food.servingSize).toInt()}, Carbs: ${food.carbohydrate * meal.quantity / food.servingSize}, Protein: ${food.protein * meal.quantity / food.servingSize}, Fat: ${food.fat * meal.quantity / food.servingSize})"
            foods.add(foodDetails)
            totalCalories += (food.calories * meal.quantity / food.servingSize).toInt()
            totalCarbs += food.carbohydrate * meal.quantity / food.servingSize
            totalProtein += food.protein * meal.quantity / food.servingSize
            totalFat += food.fat * meal.quantity / food.servingSize
        }

        Log.d("HomeViewModel", "Date: $date, Meal Type: $mealType, Foods: ${foods.joinToString(", ")}")

        val nutrientData = NutrientData(totalCalories, totalCarbs, totalProtein, totalFat)

        when (mealType) {
            "breakfast" -> {
                _breakfastNutrients.postValue(nutrientData)
                _breakfastFoodNames.postValue(foods.map { it.split(" (")[0] })
            }
            "lunch" -> {
                _lunchNutrients.postValue(nutrientData)
                _lunchFoodNames.postValue(foods.map { it.split(" (")[0] })
            }
            "dinner" -> {
                _dinnerNutrients.postValue(nutrientData)
                _dinnerFoodNames.postValue(foods.map { it.split(" (")[0] })
            }
            "snack" -> {
                _snackNutrients.postValue(nutrientData)
                _snackFoodNames.postValue(foods.map { it.split(" (")[0] })
            }
        }
    }

    // 섭취한 영양소 데이터 업데이트
    fun updateNutrientData(mealType: String, food: Food, quantity: Int) {
        val nutrientData = when (mealType) {
            "breakfast" -> _breakfastNutrients.value ?: NutrientData()
            "lunch" -> _lunchNutrients.value ?: NutrientData()
            "dinner" -> _dinnerNutrients.value ?: NutrientData()
            "snack" -> _snackNutrients.value ?: NutrientData()
            else -> NutrientData()
        }

        nutrientData.apply {
            calories += (food.calories * quantity / food.servingSize).toInt()
            carbs += (food.carbohydrate * quantity / food.servingSize)
            protein += (food.protein * quantity / food.servingSize)
            fat += (food.fat * quantity / food.servingSize)
            this.quantity += quantity
        }

        Log.d("HomeViewModel", "updateNutrientData called for mealType: $mealType, food: ${food.foodName}, quantity: $quantity")
        Log.d("HomeViewModel", "Updated nutrientData: $nutrientData")


        when (mealType) {
            "breakfast" -> _breakfastNutrients.value = nutrientData
            "lunch" -> _lunchNutrients.value = nutrientData
            "dinner" -> _dinnerNutrients.value = nutrientData
            "snack" -> _snackNutrients.value = nutrientData
        }

        updateDailyIntakeRecord()
    }


    // 섭취 기록 업데이트 메서드 추가
    private fun updateDailyIntakeRecord() {
        val totalCalories = (_breakfastNutrients.value?.calories ?: 0) +
                (_lunchNutrients.value?.calories ?: 0) +
                (_dinnerNutrients.value?.calories ?: 0) +
                (_snackNutrients.value?.calories ?: 0)

        val totalCarbs = (_breakfastNutrients.value?.carbs ?: 0f) +
                (_lunchNutrients.value?.carbs ?: 0f) +
                (_dinnerNutrients.value?.carbs ?: 0f) +
                (_snackNutrients.value?.carbs ?: 0f)

        val totalProtein = (_breakfastNutrients.value?.protein ?: 0f) +
                (_lunchNutrients.value?.protein ?: 0f) +
                (_dinnerNutrients.value?.protein ?: 0f) +
                (_snackNutrients.value?.protein ?: 0f)

        val totalFat = (_breakfastNutrients.value?.fat ?: 0f) +
                (_lunchNutrients.value?.fat ?: 0f) +
                (_dinnerNutrients.value?.fat ?: 0f) +
                (_snackNutrients.value?.fat ?: 0f)

        _currentTotalCalories.postValue(totalCalories)
        _currentCarbIntake.postValue(totalCarbs.toInt())
        _currentProteinIntake.postValue(totalProtein.toInt())
        _currentFatIntake.postValue(totalFat.toInt())

        // DailyIntakeRecord 업데이트
        val date = currentDate.value ?: return
        viewModelScope.launch {
            val record = dailyIntakeRecordRepository.getRecordByDate(date)
            val updatedRecord = record?.copy(
                currentCalories = totalCalories,
                currentCarbs = totalCarbs.toInt(),
                currentProtein = totalProtein.toInt(),
                currentFat = totalFat.toInt()
            )
            if (updatedRecord != null) {
                dailyIntakeRecordRepository.insert(updatedRecord)
                _dailyIntakeRecord.postValue(updatedRecord)
            }
        }
    }

    // 하루 섭취 목표 저장 ( 아침, 점심, 저녁, 간식 칼로리 + 탄단지 목표는 아직 구현 안함 )
    fun saveDailyIntakeGoal(
        date: String,
        targetCalories: Int,
        targetBreakfast: Int,
        targetLunch: Int,
        targetDinner: Int,
        targetSnack: Int
    ) {
        viewModelScope.launch {
            val goal = DailyIntakeGoal(
                date,
                targetCalories,
                targetBreakfast,
                targetLunch,
                targetDinner,
                targetSnack
            )
            dailyIntakeGoalRepository.insertOrUpdate(goal)
            Log.d("HomeViewModel", "Daily intake goal saved: $goal")
            _todayGoal.value = goal // 저장된 목표를 LiveData에 반영합니다.
        }
    }

    // 하루 섭취 목표 가져오기
    private fun loadDailyIntakeGoal(date: String) {
        viewModelScope.launch {
            dailyIntakeGoalRepository.getDailyIntakeGoal(date).observeForever { goal ->
                if (goal == null) {
                    // 기본값 설정
                    val defaultGoal = DailyIntakeGoal(
                        date = date,
                        targetCalories = 2600,
                        targetBreakfast = 700,
                        targetLunch = 800,
                        targetDinner = 800,
                        targetSnack = 300
                    )
                    _todayGoal.value = defaultGoal
                } else {
                    _todayGoal.value = goal
                }
            }
        }
    }

    private fun updateRemainingCalories() {
        val max = _targetCalories.value ?: 0
        val current = _currentTotalCalories.value ?: 0
        _remainingCalories.value = max - current
    }

    fun updateCurrentDate(date: LocalDate) {
        val newDate = date.format(dateFormatter)
        Log.d("HomeViewModel", "Updating current date to: $newDate")
        setCurrentDate(newDate)
        viewModelScope.launch {
            initializeDailyIntake(newDate)
            loadDailyIntakeGoal(newDate)
        }
    }

    fun updateSelectedDate(date: LocalDate) {
        val newDate = date.format(dateFormatter)
        if (_selectedDate.value != newDate) {
            _selectedDate.value = newDate
        }
    }

    data class NutrientData(
        var calories: Int = 0,
        var carbs: Float = 0f,
        var protein: Float = 0f,
        var fat: Float = 0f,
        var quantity: Float = 0f
    )


    // 추가해야 할 코드 부분
    fun refreshFilteredFoods() {
        val mealTypeValue = _mealType.value
        if (mealTypeValue != null) {
            Log.d("HomeViewModel", "Refreshing filtered foods for meal type: $mealTypeValue")
            filterFoodsByMealType(mealTypeValue)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun refreshFoodNames() {
        viewModelScope.launch {
            val date = currentDate.value ?: return@launch

            // 각 식사 유형에 대해 업데이트
            updateFoodNamesForMealType(date, "breakfast")
            updateFoodNamesForMealType(date, "lunch")
            updateFoodNamesForMealType(date, "dinner")
            updateFoodNamesForMealType(date, "snack")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun updateFoodNamesForMealType(date: String, mealType: String) {
        val meals = mealRepository.getMealsByDateAndTypeSync(date, mealType)
        val foods = meals.map { meal ->
            foodRepository.getFoodByFoodCode(meal.dietFoodCode).foodName
        }
        when (mealType) {
            "breakfast" -> _breakfastFoodNames.postValue(foods)
            "lunch" -> _lunchFoodNames.postValue(foods)
            "dinner" -> _dinnerFoodNames.postValue(foods)
            "snack" -> _snackFoodNames.postValue(foods)
        }
    }
}

