package com.smartchaindb.rfq

import com.bigchaindb.model.MetaData

data class RFQModel(val product: String,
                    val material: String,
                    val priceSpecification: Int) {

    fun metadata(): MetaData {
        val metadata = MetaData()
        metadata.setMetaData("Part Name/Description", product)
        metadata.setMetaData("Quantity", priceSpecification.toString())
        metadata.setMetaData("Material", material)
//        metadata.setMetaData("Part Volume", "1cu in");
//        metadata.setMetaData("Part color", "stock color");
//        metadata.setMetaData("Expected Delivery Time", "14days");
//        metadata.setMetaData("Manufacturing Process", "Additive Manufacturing");
//        metadata.setMetaData("Additional Services", "Protected Packaging");
        return metadata
    }
}
