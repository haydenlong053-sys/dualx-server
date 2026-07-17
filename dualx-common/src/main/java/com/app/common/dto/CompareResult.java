package com.app.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompareResult {

   private boolean userMatch;
   private boolean amountMatch;
   private boolean redemptionMatch;
   private boolean hashMatch;
   private boolean statusMatch;

    public boolean isAllMatch() {
        return userMatch && amountMatch && redemptionMatch && hashMatch && statusMatch;
    }
}