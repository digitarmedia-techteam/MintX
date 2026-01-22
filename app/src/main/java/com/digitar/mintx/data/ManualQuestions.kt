package com.digitar.mintx.data

import com.digitar.mintx.data.model.QuizQuestion

object ManualQuestions {

    private val generalQuestions = listOf(
        QuizQuestion(
            id = 1001,
            question = "Which planet is known as the Red Planet?",
            description = null,
            answers = mapOf("answer_a" to "Earth", "answer_b" to "Mars", "answer_c" to "Jupiter", "answer_d" to "Venus"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "true", "answer_c_correct" to "false", "answer_d_correct" to "false"),
            explanation = "Mars is known as the Red Planet due to iron oxide on its surface.",
            category = "General Knowledge",
            difficulty = "Easy"
        ),
        QuizQuestion(
            id = 1002,
            question = "What is the capital of France?",
            description = null,
            answers = mapOf("answer_a" to "Berlin", "answer_b" to "Madrid", "answer_c" to "Paris", "answer_d" to "Rome"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "false", "answer_c_correct" to "true", "answer_d_correct" to "false"),
            explanation = "Paris is the capital of France.",
            category = "General Knowledge",
            difficulty = "Easy"
        ),
        QuizQuestion(
            id = 1003,
            question = "Which is the largest ocean on Earth?",
            description = null,
            answers = mapOf("answer_a" to "Atlantic", "answer_b" to "Indian", "answer_c" to "Arctic", "answer_d" to "Pacific"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "false", "answer_c_correct" to "false", "answer_d_correct" to "true"),
            explanation = "The Pacific Ocean is the largest and deepest ocean on Earth.",
            category = "General Knowledge",
            difficulty = "Medium"
        ),
        QuizQuestion(
            id = 1004,
            question = "Who wrote 'Romeo and Juliet'?",
            description = null,
            answers = mapOf("answer_a" to "Charles Dickens", "answer_b" to "William Shakespeare", "answer_c" to "Mark Twain", "answer_d" to "Jane Austen"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "true", "answer_c_correct" to "false", "answer_d_correct" to "false"),
            explanation = "William Shakespeare wrote the tragedy 'Romeo and Juliet'.",
            category = "General Knowledge",
            difficulty = "Medium"
        ),
        QuizQuestion(
            id = 1005,
            question = "How many continents are there on Earth?",
            description = null,
            answers = mapOf("answer_a" to "5", "answer_b" to "6", "answer_c" to "7", "answer_d" to "8"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "false", "answer_c_correct" to "true", "answer_d_correct" to "false"),
            explanation = "There are 7 continents: Africa, Antarctica, Asia, Europe, North America, Australia, and South America.",
            category = "General Knowledge",
            difficulty = "Easy"
        )
    )

    private val scienceQuestions = listOf(
        QuizQuestion(
            id = 2001,
            question = "What is the chemical symbol for Gold?",
            description = null,
            answers = mapOf("answer_a" to "Au", "answer_b" to "Ag", "answer_c" to "Fe", "answer_d" to "Pb"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "true", "answer_b_correct" to "false", "answer_c_correct" to "false", "answer_d_correct" to "false"),
            explanation = "Au refers to Aurum, the Latin word for Gold.",
            category = "Science",
            difficulty = "Medium"
        ),
        QuizQuestion(
            id = 2002,
            question = "Who proposed the theory of relativity?",
            description = null,
            answers = mapOf("answer_a" to "Newton", "answer_b" to "Einstein", "answer_c" to "Galileo", "answer_d" to "Tesla"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "true", "answer_c_correct" to "false", "answer_d_correct" to "false"),
            explanation = "Albert Einstein formulated the theory of relativity.",
            category = "Science",
            difficulty = "Hard"
        ),
        QuizQuestion(
            id = 2003,
            question = "What is the powerhouse of the cell?",
            description = null,
            answers = mapOf("answer_a" to "Nucleus", "answer_b" to "Mitochondria", "answer_c" to "Ribosome", "answer_d" to "Golgi apparatus"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "true", "answer_c_correct" to "false", "answer_d_correct" to "false"),
            explanation = "Mitochondria are often referred to as the powerhouse of the cell.",
            category = "Science",
            difficulty = "Medium"
        ),
        QuizQuestion(
            id = 2004,
            question = "Which gas do plants absorb from the atmosphere?",
            description = null,
            answers = mapOf("answer_a" to "Oxygen", "answer_b" to "Nitrogen", "answer_c" to "Carbon Dioxide", "answer_d" to "Hydrogen"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "false", "answer_c_correct" to "true", "answer_d_correct" to "false"),
            explanation = "Plants absorb Carbon Dioxide for photosynthesis.",
            category = "Science",
            difficulty = "Easy"
        ),
        QuizQuestion(
            id = 2005,
            question = "What is the speed of light?",
            description = null,
            answers = mapOf("answer_a" to "300,000 km/s", "answer_b" to "150,000 km/s", "answer_c" to "1,000 km/s", "answer_d" to "Sound speed"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "true", "answer_b_correct" to "false", "answer_c_correct" to "false", "answer_d_correct" to "false"),
            explanation = "Light travels at approximately 299,792 kilometers per second in a vacuum.",
            category = "Science",
            difficulty = "Hard"
        )
    )

    private val sportsQuestions = listOf(
        QuizQuestion(
            id = 3001,
            question = "Which country won the FIFA World Cup in 2018?",
            description = null,
            answers = mapOf("answer_a" to "Brazil", "answer_b" to "Germany", "answer_c" to "France", "answer_d" to "Argentina"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "false", "answer_c_correct" to "true", "answer_d_correct" to "false"),
            explanation = "France defeated Croatia to win the 2018 World Cup.",
            category = "Sports",
            difficulty = "Medium"
        ),
        QuizQuestion(
            id = 3002,
            question = "How many players are in a cricket team?",
            description = null,
            answers = mapOf("answer_a" to "9", "answer_b" to "10", "answer_c" to "11", "answer_d" to "12"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "false", "answer_c_correct" to "true", "answer_d_correct" to "false"),
            explanation = "A cricket team has 11 players on the field.",
            category = "Sports",
            difficulty = "Easy"
        ),
        QuizQuestion(
            id = 3003,
            question = "In which sport is the term 'Love' used?",
            description = null,
            answers = mapOf("answer_a" to "Tennis", "answer_b" to "Badminton", "answer_c" to "Cricket", "answer_d" to "Football"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "true", "answer_b_correct" to "false", "answer_c_correct" to "false", "answer_d_correct" to "false"),
            explanation = "'Love' represents a score of zero in Tennis.",
            category = "Sports",
            difficulty = "Medium"
        ),
        QuizQuestion(
            id = 3004,
            question = "Which team has won the most NBA championships?",
            description = null,
            answers = mapOf("answer_a" to "Chicago Bulls", "answer_b" to "LA Lakers", "answer_c" to "Boston Celtics", "answer_d" to "Both B and C"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "false", "answer_c_correct" to "false", "answer_d_correct" to "true"),
            explanation = "The Celtics and Lakers are tied for the most championships.",
            category = "Sports",
            difficulty = "Hard"
        ),
        QuizQuestion(
            id = 3005,
            question = "How long is a marathon?",
            description = null,
            answers = mapOf("answer_a" to "26.2 miles", "answer_b" to "20 miles", "answer_c" to "13.1 miles", "answer_d" to "30 miles"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "true", "answer_b_correct" to "false", "answer_c_correct" to "false", "answer_d_correct" to "false"),
            explanation = "A marathon is exactly 42.195 kilometers or 26.2 miles.",
            category = "Sports",
            difficulty = "Medium"
        )
    )
    
     private val techQuestions = listOf(
        QuizQuestion(
            id = 4001,
            question = "Which operating system is developed by Google?",
            description = null,
            answers = mapOf("answer_a" to "Windows", "answer_b" to "macOS", "answer_c" to "Android", "answer_d" to "Linux"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "false", "answer_c_correct" to "true", "answer_d_correct" to "false"),
            explanation = "Android is the mobile OS developed by Google.",
            category = "Technology",
            difficulty = "Easy"
        ),
        QuizQuestion(
            id = 4002,
            question = "What does CPU stand for?",
            description = null,
            answers = mapOf("answer_a" to "Central Process Unit", "answer_b" to "Central Processing Unit", "answer_c" to "Computer Personal Unit", "answer_d" to "Central Processor Unit"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "true", "answer_c_correct" to "false", "answer_d_correct" to "false"),
            explanation = "CPU is the Central Processing Unit.",
            category = "Technology",
            difficulty = "Easy"
        ),
        QuizQuestion(
            id = 4003,
            question = "What does HTTP stand for?",
            description = null,
            answers = mapOf("answer_a" to "HyperText Transfer Protocol", "answer_b" to "HyperText Training Protocol", "answer_c" to "High Transfer Text Protocol", "answer_d" to "None of above"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "true", "answer_b_correct" to "false", "answer_c_correct" to "false", "answer_d_correct" to "false"),
            explanation = "HTTP is HyperText Transfer Protocol.",
            category = "Technology",
            difficulty = "Medium"
        ),
        QuizQuestion(
            id = 4004,
            question = "Which company makes the iPhone?",
            description = null,
            answers = mapOf("answer_a" to "Samsung", "answer_b" to "Google", "answer_c" to "Apple", "answer_d" to "Microsoft"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "false", "answer_c_correct" to "true", "answer_d_correct" to "false"),
            explanation = "Apple Inc. manufactures the iPhone.",
            category = "Technology",
            difficulty = "Easy"
        ),
        QuizQuestion(
            id = 4005,
            question = "What implies the term 'Cloud' in computing?",
            description = null,
            answers = mapOf("answer_a" to "Rainfall prediction", "answer_b" to "Internet-based servers", "answer_c" to "Internal storage", "answer_d" to "Satellite"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "true", "answer_c_correct" to "false", "answer_d_correct" to "false"),
            explanation = "Cloud computing refers to using internet-based servers.",
            category = "Technology",
            difficulty = "Medium"
        )
    )

    private val historyQuestions = listOf(
        QuizQuestion(
            id = 5001,
            question = "Who was the first President of the United States?",
            description = null,
            answers = mapOf("answer_a" to "Thomas Jefferson", "answer_b" to "Abraham Lincoln", "answer_c" to "George Washington", "answer_d" to "John Adams"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "false", "answer_c_correct" to "true", "answer_d_correct" to "false"),
            explanation = "George Washington served as the first President.",
            category = "History",
            difficulty = "Easy"
        ),
        QuizQuestion(
            id = 5002,
            question = "In which year did World War II end?",
            description = null,
            answers = mapOf("answer_a" to "1941", "answer_b" to "1945", "answer_c" to "1950", "answer_d" to "1939"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "true", "answer_c_correct" to "false", "answer_d_correct" to "false"),
            explanation = "World War II ended in 1945.",
            category = "History",
            difficulty = "Medium"
        ),
        QuizQuestion(
            id = 5003,
            question = "Who discovered America?",
            description = null,
            answers = mapOf("answer_a" to "Christopher Columbus", "answer_b" to "Vasco da Gama", "answer_c" to "James Cook", "answer_d" to "Ferdinand Magellan"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "true", "answer_b_correct" to "false", "answer_c_correct" to "false", "answer_d_correct" to "false"),
            explanation = "Christopher Columbus is credited with discovering the Americas in 1492.",
            category = "History",
            difficulty = "Easy"
        ),
        QuizQuestion(
            id = 5004,
            question = "Which ancient civilization built the Pyramids?",
            description = null,
            answers = mapOf("answer_a" to "Romans", "answer_b" to "Greeks", "answer_c" to "Egyptians", "answer_d" to "Mayans"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "false", "answer_c_correct" to "true", "answer_d_correct" to "false"),
            explanation = "The Ancient Egyptians built the pyramids.",
            category = "History",
            difficulty = "Easy"
        ),
        QuizQuestion(
            id = 5005,
            question = "Who painted the Mona Lisa?",
            description = null,
            answers = mapOf("answer_a" to "Van Gogh", "answer_b" to "Picasso", "answer_c" to "Da Vinci", "answer_d" to "Michelangelo"),
            multipleCorrectAnswers = "false",
            correctAnswers = mapOf("answer_a_correct" to "false", "answer_b_correct" to "false", "answer_c_correct" to "true", "answer_d_correct" to "false"),
            explanation = "Leonardo da Vinci painted the Mona Lisa.",
            category = "History",
            difficulty = "Medium"
        )
    )

    fun getQuestions(category: String?): List<QuizQuestion> {
        return when (category?.lowercase()) {
            "science" -> scienceQuestions
            "sports" -> sportsQuestions
            "history" -> historyQuestions
            "general knowledge", "general" -> generalQuestions
            "technology", "code", "linux", "devops", "docker", "cloud" -> techQuestions
            else -> generalQuestions + scienceQuestions + sportsQuestions + techQuestions + historyQuestions
        }
    }
}
