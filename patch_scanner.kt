                                        val symptomsList = res.symptoms.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                        if (symptomsList.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Symptoms:", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                            symptomsList.forEach {
                                                Text("• $it", color = Color(0xFFC62828), fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            val careTipsList = res.careTips.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            if (careTipsList.isNotEmpty()) {
                                Text("Actionable Care Tips", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        careTipsList.forEach { tip ->
                                            Text("• $tip", color = Color(0xFF2E7D32), fontSize = 14.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            val treatmentList = res.treatmentSteps.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            if (treatmentList.isNotEmpty()) {
                                Text("Recommended Treatment", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                treatmentList.forEachIndexed { index, step ->
                                    Row(modifier = Modifier.padding(bottom = 8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(HeroCardBg, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${index + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(step, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
