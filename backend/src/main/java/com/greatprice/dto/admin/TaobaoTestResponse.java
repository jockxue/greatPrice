package com.greatprice.dto.admin;

public record TaobaoTestResponse(
        TaobaoProbeResponse search,
        TaobaoProbeResponse detail,
        TaobaoProbeResponse promotion) {
}
