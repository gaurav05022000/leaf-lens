    private val repository: PlantRepository
    
    init {
        repository = PlantRepository.getInstance(application)
    }
