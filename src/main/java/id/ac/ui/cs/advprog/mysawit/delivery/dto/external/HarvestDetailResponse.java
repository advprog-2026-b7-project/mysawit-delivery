package id.ac.ui.cs.advprog.mysawit.delivery.dto.external;

import lombok.Getter;
import lombok.Setter;

/**
 * Response envelope for GET /api/v1/harvests/{id} — a single harvest record.
 */
@Getter
@Setter
public class HarvestDetailResponse {
    private HarvestQueryResponse.HarvestItemResponse data;
}
