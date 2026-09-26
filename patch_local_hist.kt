        for (i in 0 until count) {
            val id = prefs.getString("sh_${i}_id", "") ?: ""
            val name = prefs.getString("sh_${i}_name", "") ?: ""
            val species = prefs.getString("sh_${i}_species", "") ?: ""
            val health = prefs.getString("sh_${i}_health", "") ?: ""
            val disease = prefs.getString("sh_${i}_disease", null)
            val severity = prefs.getString("sh_${i}_severity", null)
            val ts = prefs.getLong("sh_${i}_ts", 0L)
            val imageUrl = prefs.getString("sh_${i}_image", null)
            list.add(ScanHistoryItem(id, name, species, health, disease, severity, ts, imageUrl))
        }
        _history.value = list
    }

    private fun saveLocalHistory(list: List<ScanHistoryItem>) {
        val editor = prefs.edit()
        editor.putInt("sh_count", list.size)
        list.forEachIndexed { i, item ->
            editor.putString("sh_${i}_id", item.id)
            editor.putString("sh_${i}_name", item.plantName)
            editor.putString("sh_${i}_species", item.species)
            editor.putString("sh_${i}_health", item.healthStatus)
            editor.putString("sh_${i}_disease", item.disease)
            editor.putString("sh_${i}_severity", item.severityLevel)
            editor.putLong("sh_${i}_ts", item.timestamp)
            editor.putString("sh_${i}_image", item.imageUrl)
        }
        editor.apply()
