fun main() {
    val message = "Here is a plant:\n![Image of plant](https://image.pollinations.ai/prompt/high_quality_photo_of_Monstera?width=400&height=300&nologo=true)\nIt is beautiful!"
    val imageRegex = """!\[.*?\]\((.*?)\)""".toRegex()
    val matches = imageRegex.findAll(message)
    for (match in matches) {
        println(match.groupValues[1])
    }
}
