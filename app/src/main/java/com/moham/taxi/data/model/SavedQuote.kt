package com.moham.taxi.data.model

import org.json.JSONArray
import org.json.JSONObject

data class SavedQuote(
    val origin: String,
    val destination: String,
    val dateTime: String,
    val quoteData: QuoteData
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("origin", origin)
        obj.put("destination", destination)
        obj.put("dateTime", dateTime)
        
        val qDataObj = JSONObject()
        qDataObj.put("title", quoteData.title)
        qDataObj.put("totalAmount", quoteData.totalAmount)
        
        val itemsArr = JSONArray()
        quoteData.items.forEach { item ->
            val itemObj = JSONObject()
            itemObj.put("description", item.description)
            itemObj.put("quantity", item.quantity ?: JSONObject.NULL)
            itemObj.put("unitPrice", item.unitPrice)
            itemObj.put("total", item.total)
            itemsArr.put(itemObj)
        }
        qDataObj.put("items", itemsArr)
        
        obj.put("quoteData", qDataObj)
        return obj.toString()
    }
    
    companion object {
        fun fromJson(jsonStr: String): SavedQuote {
            val obj = JSONObject(jsonStr)
            val origin = obj.getString("origin")
            val destination = obj.getString("destination")
            val dateTime = obj.getString("dateTime")
            
            val qDataObj = obj.getJSONObject("quoteData")
            val title = qDataObj.getString("title")
            val totalAmount = qDataObj.getDouble("totalAmount")
            
            val itemsArr = qDataObj.getJSONArray("items")
            val items = mutableListOf<QuoteItem>()
            for (i in 0 until itemsArr.length()) {
                val itemObj = itemsArr.getJSONObject(i)
                val desc = itemObj.getString("description")
                val qtyVal = itemObj.opt("quantity")
                val quantity = if (qtyVal == null || qtyVal == JSONObject.NULL) null else itemObj.getDouble("quantity")
                val unitPrice = itemObj.getDouble("unitPrice")
                val total = itemObj.getDouble("total")
                items.add(QuoteItem(desc, quantity, unitPrice, total))
            }
            
            return SavedQuote(
                origin = origin,
                destination = destination,
                dateTime = dateTime,
                quoteData = QuoteData(title, items, totalAmount)
            )
        }
    }
}
