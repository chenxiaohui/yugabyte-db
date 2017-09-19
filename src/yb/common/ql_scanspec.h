// Copyright (c) YugaByte, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under the License
// is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
// or implied.  See the License for the specific language governing permissions and limitations
// under the License.
//
//
// This file contains QLScanSpec that implements a QL scan (SELECT) specification.

#ifndef YB_COMMON_QL_SCANSPEC_H
#define YB_COMMON_QL_SCANSPEC_H

#include <map>

#include "yb/common/schema.h"
#include "yb/common/ql_protocol.pb.h"
#include "yb/common/ql_rowblock.h"

namespace yb {
namespace common {

// A class to determine the lower/upper-bound range components of a QL scan from its WHERE
// condition.
class QLScanRange {
 public:

  // Value range of a column
  struct QLRange {
    QLValuePB min_value;
    QLValuePB max_value;
  };

  QLScanRange(const Schema& schema, const QLConditionPB& condition);

  // Return the inclusive lower and upper range values to scan.
  // If allow_null is false and the full range group can be determined, it will be returned.
  // Otherwise, an empty group will be returned instead.
  // If allow_null is true, then returned range group could contain null values.
  // TODO(robert): allow only a subset (prefix) of range components to be specified as optimization.
  std::vector<QLValuePB> range_values(bool lower_bound, bool allow_null = false) const;

  // Interact / union / complement operators.
  QLScanRange& operator&=(const QLScanRange& other);
  QLScanRange& operator|=(const QLScanRange& other);
  QLScanRange& operator~();

  QLScanRange& operator=(QLScanRange&& other);

 private:

  // Table schema being scanned.
  const Schema& schema_;

  // Mapping of column id to the column value ranges (inclusive lower/upper bounds) to scan.
  std::unordered_map<ColumnId, QLRange> ranges_;
};


// A scan specification for a QL scan. It may be used to scan either a specified doc key
// or a hash key + optional WHERE condition clause.
class QLScanSpec {
 public:
  virtual ~QLScanSpec() {}

  // Scan for the given hash key and a condition.
  explicit QLScanSpec(const QLConditionPB* condition);

  // Evaluate the WHERE condition for the given row to decide if it is selected or not.
  // virtual to make the class polymorphic.
  virtual CHECKED_STATUS Match(const QLTableRow& table_row, bool* match) const;

 private:
  const QLConditionPB* condition_;
};

} // namespace common
} // namespace yb

#endif // YB_COMMON_QL_SCANSPEC_H