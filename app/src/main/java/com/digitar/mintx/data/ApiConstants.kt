package com.digitar.mintx.data

import com.digitar.mintx.data.model.QuizCategory
import com.digitar.mintx.data.model.SubCategory

object ApiConstants {
    const val BASE_URL = "https://quizapi.io/api/v1/"
    // Note: User should replace this with their actual API key
    const val API_KEY = "25w7Qg8oTSUSmf7UELPzjE0jUVnNpoEM2fDLmG9p"
//    const val API_KEY = ""

    val QUIZ_CATEGORIES = listOf(
        QuizCategory(
            "1", "General Knowledge", "A mix of questions from various fields.",
            subCategories = listOf(
                SubCategory("1_1", "Random"),
                SubCategory("1_2", "Daily Trivia"),
                SubCategory("1_3", "Mixed Bag")
            )
        ),
        QuizCategory(
            "2", "History", "Questions about historical events, figures, and timelines.",
            subCategories = listOf(
                SubCategory("2_1", "Ancient History"),
                SubCategory("2_2", "Modern History"),
                SubCategory("2_3", "World Wars"),
                SubCategory("2_4", "Historical Figures")
            )
        ),
        QuizCategory(
            "3", "Science", "Topics covering biology, chemistry, physics, and more.",
            subCategories = listOf(
                SubCategory("3_1", "Biology"),
                SubCategory("3_2", "Chemistry"),
                SubCategory("3_3", "Physics"),
                SubCategory("3_4", "Space Science")
            )
        ),
        QuizCategory(
            "4", "Geography", "Questions related to countries, capitals, landmarks, and maps.",
            subCategories = listOf(
                SubCategory("4_1", "Countries & Capitals"),
                SubCategory("4_2", "Landmarks"),
                SubCategory("4_3", "Physical Geography")
            )
        ),
        QuizCategory(
            "5", "Entertainment", "Includes movies, music, television shows, and celebrities.",
            subCategories = listOf(
                SubCategory("5_1", "Movies"),
                SubCategory("5_2", "Music"),
                SubCategory("5_3", "TV Shows"),
                SubCategory("5_4", "Celebrities")
            )
        ),
        QuizCategory(
            "6", "Sports", "Questions about different sports, athletes, and sporting events.",
            subCategories = listOf(
                SubCategory("6_1", "Cricket"),
                SubCategory("6_2", "Football"),
                SubCategory("6_3", "Olympics"),
                SubCategory("6_4", "Athletes")
            )
        ),
        QuizCategory(
            "7", "Food and Drink", "Trivia about cuisines, recipes, and beverages.",
            subCategories = listOf(
                SubCategory("7_1", "World Cuisines"),
                SubCategory("7_2", "Recipes"),
                SubCategory("7_3", "Beverages")
            )
        ),
        QuizCategory(
            "8", "Literature", "Questions about authors, books, and literary terms.",
            subCategories = listOf(
                SubCategory("8_1", "Authors"),
                SubCategory("8_2", "Classic Books"),
                SubCategory("8_3", "Modern Poetry")
            )
        ),
        QuizCategory(
            "9", "Art", "Topics covering famous artists, art movements, and techniques.",
            subCategories = listOf(
                SubCategory("9_1", "Famous Artists"),
                SubCategory("9_2", "Art Movements"),
                SubCategory("9_3", "Techniques")
            )
        ),
        QuizCategory(
            "10", "Technology", "Questions about gadgets, software, and technological advancements.",
            subCategories = listOf(
                SubCategory("10_1", "Gadgets"),
                SubCategory("10_2", "Software"),
                SubCategory("10_3", "Coding"),
                SubCategory("10_4", "AI & Innovations"),
                SubCategory("10_5", "Linux & OS")
            )
        ),
        QuizCategory(
            "11", "Pop Culture", "Trivia related to current trends, social media, and viral phenomena.",
            subCategories = listOf(
                SubCategory("11_1", "Viral Trends"),
                SubCategory("11_2", "Social Media"),
                SubCategory("11_3", "Memes")
            )
        ),
        QuizCategory(
            "12", "Animals", "Questions about wildlife, pets, and animal behavior.",
            subCategories = listOf(
                SubCategory("12_1", "Wildlife"),
                SubCategory("12_2", "Pets"),
                SubCategory("12_3", "Marine Life")
            )
        ),
        QuizCategory(
            "13", "Mythology", "Trivia about myths, legends, and folklore from various cultures.",
            subCategories = listOf(
                SubCategory("13_1", "Greek Mythology"),
                SubCategory("13_2", "Norse Mythology"),
                SubCategory("13_3", "Folklore")
            )
        ),
        QuizCategory(
            "14", "Politics", "Questions about political systems, leaders, and historical events.",
            subCategories = listOf(
                SubCategory("14_1", "World Leaders"),
                SubCategory("14_2", "Political Systems"),
                SubCategory("14_3", "Global Events")
            )
        ),
        QuizCategory(
            "15", "Health and Medicine", "Topics covering medical knowledge, health tips, and anatomy.",
            subCategories = listOf(
                SubCategory("15_1", "Anatomy"),
                SubCategory("15_2", "Health Tips"),
                SubCategory("15_3", "Medical Wonders")
            )
        )
    )
    
    // Legacy support for API calls
    val DEFAULT_CATEGORIES = listOf("Linux", "DevOps", "Networking", "Code", "Cloud", "Docker", "Kubernetes")
}
