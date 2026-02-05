package com.appslabs.mintx.utils

object LevelUtils {
    
    data class LevelInfo(
        val level: Int,
        val minPoints: Long,
        val maxPoints: Long, // Points required to reach NEXT level
        val currentPoints: Long,
        val progressPercent: Int,
        val pointsToNextLevel: Long
    )

    fun calculateLevelInfo(totalPoints: Long): LevelInfo {
        var level = 1
        var pointsThreshold = 30L
        
        // Loop until we find the level where totalPoints < pointsThreshold
        // Level 1: 0 to 29 (Need 30 to pass)
        // Level 2: 30 to 64 (Need 35 to pass)
        // Level 3: 65 to 104 (Need 40 to pass)
        
        var previousThreshold = 0L
        var currentLevelPointsReq = 30L

        while (totalPoints >= pointsThreshold) {
            level++
            previousThreshold = pointsThreshold
            
            // Points required for the NEXT level increases by 5
            currentLevelPointsReq = 30L + (level - 1) * 5
            pointsThreshold += currentLevelPointsReq
        }

        val pointsInLevel = totalPoints - previousThreshold
        val progress = if (currentLevelPointsReq > 0) {
            ((pointsInLevel.toDouble() / currentLevelPointsReq) * 100).toInt()
        } else {
            0
        }

        return LevelInfo(
            level = level,
            minPoints = previousThreshold,
            maxPoints = pointsThreshold,
            currentPoints = totalPoints,
            progressPercent = progress.coerceIn(0, 100),
            pointsToNextLevel = pointsThreshold - totalPoints
        )
    }
    
    /**
     * Calculate the number of questions for a quiz based on user level
     * 
     * @param level The user's current level
     * @return Number of questions (10 at level 1, +2 per level, max 35)
     * 
     * Examples:
     * - Level 1: 10 questions
     * - Level 2: 12 questions
     * - Level 3: 14 questions
     * - Level 13+: 35 questions (max)
     */
    fun calculateQuestionCount(level: Int): Int {
        val baseQuestions = 10
        val questionsPerLevel = 2
        val maxQuestions = 35
        
        val calculatedCount = baseQuestions + ((level - 1) * questionsPerLevel)
        return calculatedCount.coerceAtMost(maxQuestions)
    }
}

