<!--
SPDX-FileCopyrightText: 2026 Anton Lem <antonlem78@gmail.com>
SPDX-License-Identifier: Apache-2.0
-->

# Complete runtime comparison

Baseline: `5a121b67`; candidate: `0f6aa72d`; protocol: `printer-lab-v2-invocation-state`.
All values below are arithmetic presentations of the original JMH 1.37 results, without
rerunning, filtering or replacing observations. All 20 matched combinations are included.

- Scores use the original JMH units. An `op` is a whole batch: 11 files for `fixtures`, one
  for each synthetic workload. Higher throughput is better; lower service time and B/op
  are better. Sampled time is for a single worker with no arrival queue.
- `±` is the reported JMH confidence half-width. `error n/a` means JMH did not estimate it;
  a repeated endpoint in a profiler's raw confidence array is not proof of zero uncertainty.
  Exact confidence arrays and raw iterations/histograms remain in the linked JSON files.
- Change is `(candidate / baseline - 1) * 100`; zero or missing denominators give `n/a`.
  Ratios have no inferred confidence interval. CI overlap is not a hypothesis test.
- GC count/time are reported aggregate counters. Fixed-duration runs process different
  numbers of batches; more GC events or allocated MB/sec can accompany fewer B/op.
- Pool values are KiB and use the profiler's maximum aggregation. Totals are sums of peaks
  that need not occur together. They include prepared models and JVM overhead, and are
  neither process RSS nor retained printer size. Code-heap totals are already included in
  the overall total; do not add subtotal rows again. The per-run overview shows MiB.
- All reported sampled-time percentiles are shown. Sample secondary `p...` metrics alias the
  corresponding primary percentiles and are displayed once. Extreme percentiles/maxima
  may depend on few samples and do not establish a stable tail-latency guarantee.

## thrpt / fixtures / 1 worker

Original results: [baseline](../baseline-5a121b67-v2/throughput-t1.json), [candidate](throughput-t1.json).
Primary mean intervals: [495.804, 963.437] / [2726.287, 4377.997] ops/s.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | ops/s | 729.620 ± 233.817 | 3552.142 ± 825.855 | 386.848 |
| `gc.alloc.rate` | MB/sec | 358.767 ± 113.447 | 594.402 ± 141.211 | 65.679 |
| `gc.alloc.rate.norm` | B/op | 518486.964 ± 1500.535 | 176248.892 ± 915.701 | -66.007 |
| `gc.count` | counts | 19.000 (error n/a) | 29.000 (error n/a) | 52.632 |
| `gc.time` | ms | 141.000 (error n/a) | 198.000 (error n/a) | 40.426 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1678.750 (error n/a) | 1585.375 (error n/a) | -5.562 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 4525.750 (error n/a) | 2727.875 (error n/a) | -39.725 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 11107.250 (error n/a) | 9355.250 (error n/a) | -15.773 |
| `mempool.Compressed Class Space.used` | KiB | 3547.336 (error n/a) | 3523.336 (error n/a) | -0.677 |
| `mempool.G1 Eden Space.used` | KiB | 622592.000 (error n/a) | 623616.000 (error n/a) | 0.164 |
| `mempool.G1 Old Gen.used` | KiB | 9050.172 (error n/a) | 13887.000 (error n/a) | 53.445 |
| `mempool.G1 Survivor Space.used` | KiB | 7023.563 (error n/a) | 7145.711 (error n/a) | 1.739 |
| `mempool.Metaspace.used` | KiB | 32166.359 (error n/a) | 31709.977 (error n/a) | -1.419 |
| `mempool.total.codeheap.used` | KiB | 17235.625 (error n/a) | 13595.375 (error n/a) | -21.120 |
| `mempool.total.used` | KiB | 690790.227 (error n/a) | 692015.297 (error n/a) | 0.177 |

## thrpt / flat128 / 1 worker

Original results: [baseline](../baseline-5a121b67-v2/throughput-t1.json), [candidate](throughput-t1.json).
Primary mean intervals: [4027.462, 5227.303] / [7880.337, 11617.845] ops/s.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | ops/s | 4627.382 ± 599.920 | 9749.091 ± 1868.754 | 110.683 |
| `gc.alloc.rate` | MB/sec | 599.123 ± 77.690 | 1080.330 ± 207.254 | 80.319 |
| `gc.alloc.rate.norm` | B/op | 135988.099 ± 84.272 | 116386.349 ± 56.322 | -14.414 |
| `gc.count` | counts | 30.000 (error n/a) | 54.000 (error n/a) | 80.000 |
| `gc.time` | ms | 181.000 (error n/a) | 217.000 (error n/a) | 19.890 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1608.500 (error n/a) | 1569.500 (error n/a) | -2.425 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 2273.250 (error n/a) | 2063.750 (error n/a) | -9.216 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 7551.750 (error n/a) | 7252.375 (error n/a) | -3.964 |
| `mempool.Compressed Class Space.used` | KiB | 3140.656 (error n/a) | 3137.133 (error n/a) | -0.112 |
| `mempool.G1 Eden Space.used` | KiB | 624640.000 (error n/a) | 627712.000 (error n/a) | 0.492 |
| `mempool.G1 Old Gen.used` | KiB | 14206.000 (error n/a) | 15025.914 (error n/a) | 5.772 |
| `mempool.G1 Survivor Space.used` | KiB | 5889.258 (error n/a) | 6144.000 (error n/a) | 4.326 |
| `mempool.Metaspace.used` | KiB | 29305.875 (error n/a) | 29305.078 (error n/a) | -0.003 |
| `mempool.total.codeheap.used` | KiB | 11328.000 (error n/a) | 10848.125 (error n/a) | -4.236 |
| `mempool.total.used` | KiB | 688020.766 (error n/a) | 691680.297 (error n/a) | 0.532 |

## thrpt / flat1024 / 1 worker

Original results: [baseline](../baseline-5a121b67-v2/throughput-t1.json), [candidate](throughput-t1.json).
Primary mean intervals: [440.883, 554.882] / [723.477, 1254.317] ops/s.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | ops/s | 497.883 ± 57.000 | 988.897 ± 265.420 | 98.620 |
| `gc.alloc.rate` | MB/sec | 490.654 ± 55.435 | 865.227 ± 231.901 | 76.341 |
| `gc.alloc.rate.norm` | B/op | 1038317.129 ± 5214.351 | 921414.694 ± 2439.253 | -11.259 |
| `gc.count` | counts | 23.000 (error n/a) | 42.000 (error n/a) | 82.609 |
| `gc.time` | ms | 149.000 (error n/a) | 140.000 (error n/a) | -6.040 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1611.125 (error n/a) | 1602.875 (error n/a) | -0.512 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 2755.875 (error n/a) | 2549.625 (error n/a) | -7.484 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 8911.250 (error n/a) | 8620.250 (error n/a) | -3.266 |
| `mempool.Compressed Class Space.used` | KiB | 3143.023 (error n/a) | 3138.094 (error n/a) | -0.157 |
| `mempool.G1 Eden Space.used` | KiB | 627712.000 (error n/a) | 627712.000 (error n/a) | 0.000 |
| `mempool.G1 Old Gen.used` | KiB | 14970.219 (error n/a) | 15262.453 (error n/a) | 1.952 |
| `mempool.G1 Survivor Space.used` | KiB | 6416.266 (error n/a) | 6311.930 (error n/a) | -1.626 |
| `mempool.Metaspace.used` | KiB | 29813.188 (error n/a) | 29802.242 (error n/a) | -0.037 |
| `mempool.total.codeheap.used` | KiB | 13126.250 (error n/a) | 12666.750 (error n/a) | -3.501 |
| `mempool.total.used` | KiB | 692673.469 (error n/a) | 694127.180 (error n/a) | 0.210 |

## thrpt / nested128 / 1 worker

Original results: [baseline](../baseline-5a121b67-v2/throughput-t1.json), [candidate](throughput-t1.json).
Primary mean intervals: [305.561, 419.310] / [625.706, 906.120] ops/s.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | ops/s | 362.436 ± 56.874 | 765.913 ± 140.207 | 111.324 |
| `gc.alloc.rate` | MB/sec | 457.081 ± 73.728 | 903.811 ± 165.027 | 97.735 |
| `gc.alloc.rate.norm` | B/op | 1334445.476 ± 13667.187 | 1244877.531 ± 5456.873 | -6.712 |
| `gc.count` | counts | 23.000 (error n/a) | 45.000 (error n/a) | 95.652 |
| `gc.time` | ms | 149.000 (error n/a) | 200.000 (error n/a) | 34.228 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1599.125 (error n/a) | 1595.750 (error n/a) | -0.211 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 2455.375 (error n/a) | 2466.375 (error n/a) | 0.448 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 9491.000 (error n/a) | 9438.125 (error n/a) | -0.557 |
| `mempool.Compressed Class Space.used` | KiB | 3144.102 (error n/a) | 3142.438 (error n/a) | -0.053 |
| `mempool.G1 Eden Space.used` | KiB | 622592.000 (error n/a) | 627712.000 (error n/a) | 0.822 |
| `mempool.G1 Old Gen.used` | KiB | 8945.375 (error n/a) | 15425.820 (error n/a) | 72.445 |
| `mempool.G1 Survivor Space.used` | KiB | 6676.906 (error n/a) | 6277.875 (error n/a) | -5.976 |
| `mempool.Metaspace.used` | KiB | 29940.883 (error n/a) | 29940.000 (error n/a) | -0.003 |
| `mempool.total.codeheap.used` | KiB | 13507.250 (error n/a) | 13315.875 (error n/a) | -1.417 |
| `mempool.total.used` | KiB | 684481.602 (error n/a) | 695040.180 (error n/a) | 1.543 |

## thrpt / fixtures / 2 workers

Original results: [baseline](../baseline-5a121b67-v2/throughput-t2.json), [candidate](throughput-t2.json).
Primary mean intervals: [918.336, 1653.826] / [5031.320, 8013.823] ops/s.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | ops/s | 1286.081 ± 367.745 | 6522.571 ± 1491.251 | 407.167 |
| `gc.alloc.rate` | MB/sec | 634.537 ± 179.404 | 1102.427 ± 252.850 | 73.737 |
| `gc.alloc.rate.norm` | B/op | 520784.064 ± 1897.065 | 178061.824 ± 580.774 | -65.809 |
| `gc.count` | counts | 32.000 (error n/a) | 55.000 (error n/a) | 71.875 |
| `gc.time` | ms | 283.000 (error n/a) | 262.000 (error n/a) | -7.420 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1654.875 (error n/a) | 1588.375 (error n/a) | -4.018 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 5042.500 (error n/a) | 3025.625 (error n/a) | -39.998 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 12203.000 (error n/a) | 10643.750 (error n/a) | -12.778 |
| `mempool.Compressed Class Space.used` | KiB | 3832.539 (error n/a) | 3791.914 (error n/a) | -1.060 |
| `mempool.G1 Eden Space.used` | KiB | 620544.000 (error n/a) | 627712.000 (error n/a) | 1.155 |
| `mempool.G1 Old Gen.used` | KiB | 10940.789 (error n/a) | 16687.102 (error n/a) | 52.522 |
| `mempool.G1 Survivor Space.used` | KiB | 8362.125 (error n/a) | 8032.844 (error n/a) | -3.938 |
| `mempool.Metaspace.used` | KiB | 33288.813 (error n/a) | 32729.156 (error n/a) | -1.681 |
| `mempool.total.codeheap.used` | KiB | 18645.875 (error n/a) | 15257.750 (error n/a) | -18.171 |
| `mempool.total.used` | KiB | 695190.570 (error n/a) | 699483.375 (error n/a) | 0.618 |

## thrpt / flat128 / 2 workers

Original results: [baseline](../baseline-5a121b67-v2/throughput-t2.json), [candidate](throughput-t2.json).
Primary mean intervals: [7007.282, 9681.916] / [11837.110, 17394.687] ops/s.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | ops/s | 8344.599 ± 1337.317 | 14615.899 ± 2778.788 | 75.154 |
| `gc.alloc.rate` | MB/sec | 1069.010 ± 167.552 | 1619.955 ± 307.597 | 51.538 |
| `gc.alloc.rate.norm` | B/op | 134634.001 ± 1073.862 | 116418.907 ± 54.188 | -13.529 |
| `gc.count` | counts | 53.000 (error n/a) | 81.000 (error n/a) | 52.830 |
| `gc.time` | ms | 233.000 (error n/a) | 284.000 (error n/a) | 21.888 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1562.750 (error n/a) | 1575.000 (error n/a) | 0.784 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 2472.250 (error n/a) | 2374.125 (error n/a) | -3.969 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 8043.125 (error n/a) | 7854.375 (error n/a) | -2.347 |
| `mempool.Compressed Class Space.used` | KiB | 3374.406 (error n/a) | 3357.063 (error n/a) | -0.514 |
| `mempool.G1 Eden Space.used` | KiB | 627712.000 (error n/a) | 627712.000 (error n/a) | 0.000 |
| `mempool.G1 Old Gen.used` | KiB | 15057.852 (error n/a) | 15150.773 (error n/a) | 0.617 |
| `mempool.G1 Survivor Space.used` | KiB | 6896.563 (error n/a) | 6560.555 (error n/a) | -4.872 |
| `mempool.Metaspace.used` | KiB | 30116.922 (error n/a) | 30107.258 (error n/a) | -0.032 |
| `mempool.total.codeheap.used` | KiB | 12066.125 (error n/a) | 11792.375 (error n/a) | -2.269 |
| `mempool.total.used` | KiB | 694423.172 (error n/a) | 693949.750 (error n/a) | -0.068 |

## thrpt / flat1024 / 2 workers

Original results: [baseline](../baseline-5a121b67-v2/throughput-t2.json), [candidate](throughput-t2.json).
Primary mean intervals: [758.321, 1130.545] / [1482.197, 2199.787] ops/s.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | ops/s | 944.433 ± 186.112 | 1840.992 ± 358.795 | 94.931 |
| `gc.alloc.rate` | MB/sec | 929.828 ± 182.755 | 1611.138 ± 314.717 | 73.273 |
| `gc.alloc.rate.norm` | B/op | 1038266.191 ± 5612.580 | 921628.965 ± 2939.419 | -11.234 |
| `gc.count` | counts | 47.000 (error n/a) | 81.000 (error n/a) | 72.340 |
| `gc.time` | ms | 111.000 (error n/a) | 156.000 (error n/a) | 40.541 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1606.875 (error n/a) | 1609.000 (error n/a) | 0.132 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 2983.125 (error n/a) | 2796.375 (error n/a) | -6.260 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 9538.250 (error n/a) | 9386.375 (error n/a) | -1.592 |
| `mempool.Compressed Class Space.used` | KiB | 3369.555 (error n/a) | 3362.250 (error n/a) | -0.217 |
| `mempool.G1 Eden Space.used` | KiB | 627712.000 (error n/a) | 627712.000 (error n/a) | 0.000 |
| `mempool.G1 Old Gen.used` | KiB | 16575.031 (error n/a) | 16359.148 (error n/a) | -1.302 |
| `mempool.G1 Survivor Space.used` | KiB | 5625.688 (error n/a) | 553.320 (error n/a) | -90.164 |
| `mempool.Metaspace.used` | KiB | 30610.633 (error n/a) | 30515.242 (error n/a) | -0.312 |
| `mempool.total.codeheap.used` | KiB | 14084.750 (error n/a) | 13668.125 (error n/a) | -2.958 |
| `mempool.total.used` | KiB | 696154.109 (error n/a) | 691542.680 (error n/a) | -0.662 |

## thrpt / nested128 / 2 workers

Original results: [baseline](../baseline-5a121b67-v2/throughput-t2.json), [candidate](throughput-t2.json).
Primary mean intervals: [492.629, 833.281] / [1093.270, 1460.645] ops/s.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | ops/s | 662.955 ± 170.326 | 1276.957 ± 183.688 | 92.616 |
| `gc.alloc.rate` | MB/sec | 842.407 ± 218.340 | 1506.650 ± 216.857 | 78.851 |
| `gc.alloc.rate.norm` | B/op | 1346275.953 ± 15302.890 | 1245997.806 ± 6400.114 | -7.449 |
| `gc.count` | counts | 42.000 (error n/a) | 76.000 (error n/a) | 80.952 |
| `gc.time` | ms | 223.000 (error n/a) | 196.000 (error n/a) | -12.108 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1604.250 (error n/a) | 1600.375 (error n/a) | -0.242 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 2854.875 (error n/a) | 2565.000 (error n/a) | -10.154 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 10334.750 (error n/a) | 9183.750 (error n/a) | -11.137 |
| `mempool.Compressed Class Space.used` | KiB | 3344.422 (error n/a) | 3362.523 (error n/a) | 0.541 |
| `mempool.G1 Eden Space.used` | KiB | 627712.000 (error n/a) | 627712.000 (error n/a) | 0.000 |
| `mempool.G1 Old Gen.used` | KiB | 16273.625 (error n/a) | 16708.273 (error n/a) | 2.671 |
| `mempool.G1 Survivor Space.used` | KiB | 7851.742 (error n/a) | 7715.727 (error n/a) | -1.732 |
| `mempool.Metaspace.used` | KiB | 30759.914 (error n/a) | 30706.984 (error n/a) | -0.172 |
| `mempool.total.codeheap.used` | KiB | 14754.750 (error n/a) | 13325.875 (error n/a) | -9.684 |
| `mempool.total.used` | KiB | 698408.195 (error n/a) | 698914.797 (error n/a) | 0.073 |

## thrpt / fixtures / 4 workers

Original results: [baseline](../baseline-5a121b67-v2/throughput-t4.json), [candidate](throughput-t4.json).
Primary mean intervals: [1315.572, 2665.849] / [7831.502, 11990.968] ops/s.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | ops/s | 1990.710 ± 675.138 | 9911.235 ± 2079.733 | 397.874 |
| `gc.alloc.rate` | MB/sec | 976.763 ± 329.697 | 1673.829 ± 352.771 | 71.365 |
| `gc.alloc.rate.norm` | B/op | 521707.198 ± 2599.778 | 178123.011 ± 630.396 | -65.858 |
| `gc.count` | counts | 51.000 (error n/a) | 82.000 (error n/a) | 60.784 |
| `gc.time` | ms | 394.000 (error n/a) | 298.000 (error n/a) | -24.365 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1666.750 (error n/a) | 1591.750 (error n/a) | -4.500 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 5499.500 (error n/a) | 3536.125 (error n/a) | -35.701 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 14582.000 (error n/a) | 12186.375 (error n/a) | -16.429 |
| `mempool.Compressed Class Space.used` | KiB | 4336.828 (error n/a) | 4323.039 (error n/a) | -0.318 |
| `mempool.G1 Eden Space.used` | KiB | 627712.000 (error n/a) | 627712.000 (error n/a) | 0.000 |
| `mempool.G1 Old Gen.used` | KiB | 18347.344 (error n/a) | 18251.250 (error n/a) | -0.524 |
| `mempool.G1 Survivor Space.used` | KiB | 10457.070 (error n/a) | 10240.000 (error n/a) | -2.076 |
| `mempool.Metaspace.used` | KiB | 35173.836 (error n/a) | 34660.984 (error n/a) | -1.458 |
| `mempool.total.codeheap.used` | KiB | 21684.625 (error n/a) | 17311.375 (error n/a) | -20.168 |
| `mempool.total.used` | KiB | 714915.516 (error n/a) | 710421.086 (error n/a) | -0.629 |

## thrpt / flat128 / 4 workers

Original results: [baseline](../baseline-5a121b67-v2/throughput-t4.json), [candidate](throughput-t4.json).
Primary mean intervals: [9930.569, 14371.496] / [19492.369, 25356.777] ops/s.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | ops/s | 12151.033 ± 2220.464 | 22424.573 ± 2932.204 | 84.549 |
| `gc.alloc.rate` | MB/sec | 1558.099 ± 289.972 | 2484.593 ± 324.741 | 59.463 |
| `gc.alloc.rate.norm` | B/op | 134696.205 ± 1068.451 | 116468.662 ± 56.423 | -13.532 |
| `gc.count` | counts | 76.000 (error n/a) | 123.000 (error n/a) | 61.842 |
| `gc.time` | ms | 270.000 (error n/a) | 272.000 (error n/a) | 0.741 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1565.625 (error n/a) | 1568.250 (error n/a) | 0.168 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 2836.000 (error n/a) | 2635.125 (error n/a) | -7.083 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 8674.875 (error n/a) | 8143.125 (error n/a) | -6.130 |
| `mempool.Compressed Class Space.used` | KiB | 3721.148 (error n/a) | 3777.945 (error n/a) | 1.526 |
| `mempool.G1 Eden Space.used` | KiB | 627712.000 (error n/a) | 627712.000 (error n/a) | 0.000 |
| `mempool.G1 Old Gen.used` | KiB | 15530.078 (error n/a) | 15708.836 (error n/a) | 1.151 |
| `mempool.G1 Survivor Space.used` | KiB | 8112.344 (error n/a) | 7819.406 (error n/a) | -3.611 |
| `mempool.Metaspace.used` | KiB | 31416.039 (error n/a) | 31537.789 (error n/a) | 0.388 |
| `mempool.total.codeheap.used` | KiB | 13069.750 (error n/a) | 12170.625 (error n/a) | -6.879 |
| `mempool.total.used` | KiB | 698007.250 (error n/a) | 698324.648 (error n/a) | 0.045 |

## thrpt / flat1024 / 4 workers

Original results: [baseline](../baseline-5a121b67-v2/throughput-t4.json), [candidate](throughput-t4.json).
Primary mean intervals: [1241.962, 1646.097] / [2061.315, 2836.250] ops/s.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | ops/s | 1444.030 ± 202.067 | 2448.782 ± 387.467 | 69.580 |
| `gc.alloc.rate` | MB/sec | 1428.507 ± 196.304 | 2140.763 ± 339.202 | 49.860 |
| `gc.alloc.rate.norm` | B/op | 1045014.767 ± 11835.905 | 922139.389 ± 4020.151 | -11.758 |
| `gc.count` | counts | 71.000 (error n/a) | 106.000 (error n/a) | 49.296 |
| `gc.time` | ms | 173.000 (error n/a) | 223.000 (error n/a) | 28.902 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1606.750 (error n/a) | 1604.750 (error n/a) | -0.124 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 3701.750 (error n/a) | 3267.375 (error n/a) | -11.734 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 10056.125 (error n/a) | 9919.625 (error n/a) | -1.357 |
| `mempool.Compressed Class Space.used` | KiB | 3728.398 (error n/a) | 3710.922 (error n/a) | -0.469 |
| `mempool.G1 Eden Space.used` | KiB | 627712.000 (error n/a) | 627712.000 (error n/a) | 0.000 |
| `mempool.G1 Old Gen.used` | KiB | 20252.289 (error n/a) | 20035.172 (error n/a) | -1.072 |
| `mempool.G1 Survivor Space.used` | KiB | 2338.883 (error n/a) | 2048.000 (error n/a) | -12.437 |
| `mempool.Metaspace.used` | KiB | 31754.492 (error n/a) | 31755.313 (error n/a) | 0.003 |
| `mempool.total.codeheap.used` | KiB | 15135.125 (error n/a) | 14760.625 (error n/a) | -2.474 |
| `mempool.total.used` | KiB | 700620.945 (error n/a) | 699516.852 (error n/a) | -0.158 |

## thrpt / nested128 / 4 workers

Original results: [baseline](../baseline-5a121b67-v2/throughput-t4.json), [candidate](throughput-t4.json).
Primary mean intervals: [1054.869, 1386.648] / [1304.470, 2109.325] ops/s.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | ops/s | 1220.759 ± 165.890 | 1706.897 ± 402.428 | 39.823 |
| `gc.alloc.rate` | MB/sec | 1551.081 ± 207.681 | 2010.290 ± 469.921 | 29.606 |
| `gc.alloc.rate.norm` | B/op | 1346467.936 ± 12954.923 | 1246991.256 ± 8676.153 | -7.388 |
| `gc.count` | counts | 78.000 (error n/a) | 101.000 (error n/a) | 29.487 |
| `gc.time` | ms | 251.000 (error n/a) | 221.000 (error n/a) | -11.952 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1606.125 (error n/a) | 1614.250 (error n/a) | 0.506 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 3133.000 (error n/a) | 2918.500 (error n/a) | -6.846 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 10811.375 (error n/a) | 10741.125 (error n/a) | -0.650 |
| `mempool.Compressed Class Space.used` | KiB | 3732.469 (error n/a) | 3751.227 (error n/a) | 0.503 |
| `mempool.G1 Eden Space.used` | KiB | 627712.000 (error n/a) | 627712.000 (error n/a) | 0.000 |
| `mempool.G1 Old Gen.used` | KiB | 18057.195 (error n/a) | 18340.391 (error n/a) | 1.568 |
| `mempool.G1 Survivor Space.used` | KiB | 9915.977 (error n/a) | 8036.500 (error n/a) | -18.954 |
| `mempool.Metaspace.used` | KiB | 32232.039 (error n/a) | 32205.305 (error n/a) | -0.083 |
| `mempool.total.codeheap.used` | KiB | 15524.000 (error n/a) | 15172.625 (error n/a) | -2.263 |
| `mempool.total.used` | KiB | 706726.602 (error n/a) | 704784.570 (error n/a) | -0.275 |

## thrpt / fixtures / 8 workers

Original results: [baseline](../baseline-5a121b67-v2/throughput-t8.json), [candidate](throughput-t8.json).
Primary mean intervals: [1993.039, 3838.197] / [10574.121, 13171.981] ops/s.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | ops/s | 2915.618 ± 922.579 | 11873.051 ± 1298.930 | 307.222 |
| `gc.alloc.rate` | MB/sec | 1381.126 ± 484.874 | 1991.143 ± 213.446 | 44.168 |
| `gc.alloc.rate.norm` | B/op | 521172.301 ± 2993.284 | 177979.621 ± 769.698 | -65.850 |
| `gc.count` | counts | 77.000 (error n/a) | 104.000 (error n/a) | 35.065 |
| `gc.time` | ms | 474.000 (error n/a) | 305.000 (error n/a) | -35.654 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1672.500 (error n/a) | 1606.375 (error n/a) | -3.954 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 5630.750 (error n/a) | 3938.875 (error n/a) | -30.047 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 14545.875 (error n/a) | 12426.750 (error n/a) | -14.569 |
| `mempool.Compressed Class Space.used` | KiB | 5294.898 (error n/a) | 5231.664 (error n/a) | -1.194 |
| `mempool.G1 Eden Space.used` | KiB | 627712.000 (error n/a) | 627712.000 (error n/a) | 0.000 |
| `mempool.G1 Old Gen.used` | KiB | 21798.430 (error n/a) | 21535.625 (error n/a) | -1.206 |
| `mempool.G1 Survivor Space.used` | KiB | 14120.836 (error n/a) | 12865.156 (error n/a) | -8.892 |
| `mempool.Metaspace.used` | KiB | 38410.797 (error n/a) | 38083.516 (error n/a) | -0.852 |
| `mempool.total.codeheap.used` | KiB | 21779.750 (error n/a) | 17936.250 (error n/a) | -17.647 |
| `mempool.total.used` | KiB | 726675.445 (error n/a) | 721268.219 (error n/a) | -0.744 |

## thrpt / flat128 / 8 workers

Original results: [baseline](../baseline-5a121b67-v2/throughput-t8.json), [candidate](throughput-t8.json).
Primary mean intervals: [14227.762, 17214.433] / [26013.541, 29833.208] ops/s.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | ops/s | 15721.098 ± 1493.336 | 27923.374 ± 1909.833 | 77.617 |
| `gc.alloc.rate` | MB/sec | 2025.410 ± 194.452 | 3081.411 ± 215.328 | 52.138 |
| `gc.alloc.rate.norm` | B/op | 135931.190 ± 174.591 | 116384.702 ± 95.098 | -14.380 |
| `gc.count` | counts | 105.000 (error n/a) | 158.000 (error n/a) | 50.476 |
| `gc.time` | ms | 330.000 (error n/a) | 347.000 (error n/a) | 5.152 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1574.500 (error n/a) | 1575.875 (error n/a) | 0.087 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 3220.500 (error n/a) | 2977.750 (error n/a) | -7.538 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 8991.250 (error n/a) | 8686.625 (error n/a) | -3.388 |
| `mempool.Compressed Class Space.used` | KiB | 4555.797 (error n/a) | 4498.813 (error n/a) | -1.251 |
| `mempool.G1 Eden Space.used` | KiB | 627712.000 (error n/a) | 627712.000 (error n/a) | 0.000 |
| `mempool.G1 Old Gen.used` | KiB | 17512.844 (error n/a) | 17787.883 (error n/a) | 1.570 |
| `mempool.G1 Survivor Space.used` | KiB | 9168.250 (error n/a) | 279.875 (error n/a) | -96.947 |
| `mempool.Metaspace.used` | KiB | 34211.164 (error n/a) | 34057.969 (error n/a) | -0.448 |
| `mempool.total.codeheap.used` | KiB | 13761.125 (error n/a) | 13229.125 (error n/a) | -3.866 |
| `mempool.total.used` | KiB | 706350.313 (error n/a) | 697201.539 (error n/a) | -1.295 |

## thrpt / flat1024 / 8 workers

Original results: [baseline](../baseline-5a121b67-v2/throughput-t8.json), [candidate](throughput-t8.json).
Primary mean intervals: [1594.708, 1885.690] / [2231.693, 3650.022] ops/s.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | ops/s | 1740.199 ± 145.491 | 2940.858 ± 709.165 | 68.996 |
| `gc.alloc.rate` | MB/sec | 1762.637 ± 162.116 | 2558.169 ± 628.220 | 45.133 |
| `gc.alloc.rate.norm` | B/op | 1073874.287 ± 28663.740 | 923839.653 ± 8542.993 | -13.971 |
| `gc.count` | counts | 93.000 (error n/a) | 133.000 (error n/a) | 43.011 |
| `gc.time` | ms | 283.000 (error n/a) | 338.000 (error n/a) | 19.435 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1606.375 (error n/a) | 1587.750 (error n/a) | -1.159 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 4270.250 (error n/a) | 3957.750 (error n/a) | -7.318 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 10523.375 (error n/a) | 10246.000 (error n/a) | -2.636 |
| `mempool.Compressed Class Space.used` | KiB | 4413.422 (error n/a) | 4411.500 (error n/a) | -0.044 |
| `mempool.G1 Eden Space.used` | KiB | 627712.000 (error n/a) | 627712.000 (error n/a) | 0.000 |
| `mempool.G1 Old Gen.used` | KiB | 26380.898 (error n/a) | 26356.148 (error n/a) | -0.094 |
| `mempool.G1 Survivor Space.used` | KiB | 4177.250 (error n/a) | 530.969 (error n/a) | -87.289 |
| `mempool.Metaspace.used` | KiB | 34064.273 (error n/a) | 33957.492 (error n/a) | -0.313 |
| `mempool.total.codeheap.used` | KiB | 16241.375 (error n/a) | 15495.500 (error n/a) | -4.592 |
| `mempool.total.used` | KiB | 710901.164 (error n/a) | 708108.438 (error n/a) | -0.393 |

## thrpt / nested128 / 8 workers

Original results: [baseline](../baseline-5a121b67-v2/throughput-t8.json), [candidate](throughput-t8.json).
Primary mean intervals: [1322.980, 1580.143] / [2123.731, 2448.328] ops/s.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | ops/s | 1451.562 ± 128.582 | 2286.030 ± 162.299 | 57.488 |
| `gc.alloc.rate` | MB/sec | 1834.121 ± 165.580 | 2683.456 ± 192.210 | 46.307 |
| `gc.alloc.rate.norm` | B/op | 1350783.819 ± 22611.969 | 1249219.368 ± 13561.046 | -7.519 |
| `gc.count` | counts | 96.000 (error n/a) | 138.000 (error n/a) | 43.750 |
| `gc.time` | ms | 218.000 (error n/a) | 325.000 (error n/a) | 49.083 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1611.500 (error n/a) | 1586.875 (error n/a) | -1.528 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 3502.250 (error n/a) | 3399.750 (error n/a) | -2.927 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 11384.875 (error n/a) | 11104.000 (error n/a) | -2.467 |
| `mempool.Compressed Class Space.used` | KiB | 4493.758 (error n/a) | 4449.008 (error n/a) | -0.996 |
| `mempool.G1 Eden Space.used` | KiB | 627712.000 (error n/a) | 627712.000 (error n/a) | 0.000 |
| `mempool.G1 Old Gen.used` | KiB | 23835.969 (error n/a) | 23571.531 (error n/a) | -1.109 |
| `mempool.G1 Survivor Space.used` | KiB | 2249.195 (error n/a) | 394.563 (error n/a) | -82.458 |
| `mempool.Metaspace.used` | KiB | 34685.727 (error n/a) | 34567.609 (error n/a) | -0.341 |
| `mempool.total.codeheap.used` | KiB | 16466.375 (error n/a) | 16009.750 (error n/a) | -2.773 |
| `mempool.total.used` | KiB | 707231.578 (error n/a) | 706578.305 (error n/a) | -0.092 |

## sample / fixtures / 1 worker

Original results: [baseline](../baseline-5a121b67-v2/sample-t1.json), [candidate](sample-t1.json).
Primary mean intervals: [1449.234, 1509.595] / [241.322, 248.333] us/op.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | us/op | 1479.414 ± 30.180 | 244.828 ± 3.505 | -83.451 |
| `gc.alloc.rate` | MB/sec | 331.616 ± 140.005 | 678.340 ± 192.268 | 104.556 |
| `gc.alloc.rate.norm` | B/op | 519897.754 ± 2862.608 | 175806.549 ± 231.986 | -66.184 |
| `gc.count` | counts | 18.000 (error n/a) | 33.000 (error n/a) | 83.333 |
| `gc.time` | ms | 153.000 (error n/a) | 223.000 (error n/a) | 45.752 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1704.250 (error n/a) | 1633.125 (error n/a) | -4.173 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 4475.875 (error n/a) | 2812.625 (error n/a) | -37.160 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 11620.875 (error n/a) | 9096.625 (error n/a) | -21.722 |
| `mempool.Compressed Class Space.used` | KiB | 3554.523 (error n/a) | 3528.313 (error n/a) | -0.737 |
| `mempool.G1 Eden Space.used` | KiB | 621568.000 (error n/a) | 626688.000 (error n/a) | 0.824 |
| `mempool.G1 Old Gen.used` | KiB | 8835.734 (error n/a) | 15465.336 (error n/a) | 75.032 |
| `mempool.G1 Survivor Space.used` | KiB | 7625.500 (error n/a) | 7714.906 (error n/a) | 1.172 |
| `mempool.Metaspace.used` | KiB | 32286.695 (error n/a) | 31687.594 (error n/a) | -1.856 |
| `mempool.total.codeheap.used` | KiB | 17717.875 (error n/a) | 13495.000 (error n/a) | -23.834 |
| `mempool.total.used` | KiB | 691105.344 (error n/a) | 693309.289 (error n/a) | 0.319 |

| Percentile | Baseline us/batch | Candidate us/batch | Change % |
| --- | ---: | ---: | ---: |
| p0.0 | 589.824 | 120.960 | -79.492 |
| p50.0 | 1105.920 | 173.056 | -84.352 |
| p90.0 | 2617.344 | 415.744 | -84.116 |
| p95.0 | 3432.448 | 501.760 | -85.382 |
| p99.0 | 6186.598 | 860.160 | -86.096 |
| p99.9 | 16453.468 | 2976.055 | -81.912 |
| p99.99 | 37591.450 | 9377.409 | -75.054 |
| p99.999 | 42860.544 | 76381.414 | 78.209 |
| p99.9999 | 42860.544 | 91226.112 | 112.844 |
| p100.0 | 42860.544 | 91226.112 | 112.844 |

## sample / flat128 / 1 worker

Original results: [baseline](../baseline-5a121b67-v2/sample-t1.json), [candidate](sample-t1.json).
Primary mean intervals: [172.975, 304.520] / [102.186, 177.043] us/op.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | us/op | 238.747 ± 65.773 | 139.615 ± 37.429 | -41.522 |
| `gc.alloc.rate` | MB/sec | 561.730 ± 168.127 | 821.457 ± 357.015 | 46.237 |
| `gc.alloc.rate.norm` | B/op | 135312.596 ± 1114.187 | 116440.791 ± 83.445 | -13.947 |
| `gc.count` | counts | 29.000 (error n/a) | 42.000 (error n/a) | 44.828 |
| `gc.time` | ms | 181.000 (error n/a) | 216.000 (error n/a) | 19.337 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1613.250 (error n/a) | 1607.375 (error n/a) | -0.364 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 2456.125 (error n/a) | 2199.000 (error n/a) | -10.469 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 7601.000 (error n/a) | 7229.375 (error n/a) | -4.889 |
| `mempool.Compressed Class Space.used` | KiB | 3146.742 (error n/a) | 3143.383 (error n/a) | -0.107 |
| `mempool.G1 Eden Space.used` | KiB | 623616.000 (error n/a) | 627712.000 (error n/a) | 0.657 |
| `mempool.G1 Old Gen.used` | KiB | 14147.305 (error n/a) | 15250.242 (error n/a) | 7.796 |
| `mempool.G1 Survivor Space.used` | KiB | 6850.016 (error n/a) | 6891.633 (error n/a) | 0.608 |
| `mempool.Metaspace.used` | KiB | 29410.414 (error n/a) | 29354.586 (error n/a) | -0.190 |
| `mempool.total.codeheap.used` | KiB | 11521.000 (error n/a) | 10981.375 (error n/a) | -4.684 |
| `mempool.total.used` | KiB | 688109.109 (error n/a) | 690727.125 (error n/a) | 0.380 |

| Percentile | Baseline us/batch | Candidate us/batch | Change % |
| --- | ---: | ---: | ---: |
| p0.0 | 131.840 | 57.792 | -56.165 |
| p50.0 | 183.296 | 83.840 | -54.260 |
| p90.0 | 321.536 | 194.560 | -39.490 |
| p95.0 | 371.200 | 238.336 | -35.793 |
| p99.0 | 568.822 | 472.064 | -17.010 |
| p99.9 | 1695.840 | 3006.530 | 77.288 |
| p99.99 | 8240.520 | 20303.289 | 146.384 |
| p99.999 | 1791878.011 | 120752.848 | -93.261 |
| p99.9999 | 2650800.128 | 2638217.216 | -0.475 |
| p100.0 | 2650800.128 | 2638217.216 | -0.475 |

## sample / flat1024 / 1 worker

Original results: [baseline](../baseline-5a121b67-v2/sample-t1.json), [candidate](sample-t1.json).
Primary mean intervals: [2003.876, 2055.410] / [1049.134, 1076.218] us/op.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | us/op | 2029.643 ± 25.767 | 1062.676 ± 13.542 | -47.642 |
| `gc.alloc.rate` | MB/sec | 484.446 ± 95.458 | 820.208 ± 182.288 | 69.308 |
| `gc.alloc.rate.norm` | B/op | 1038863.667 ± 6266.030 | 921614.655 ± 2611.107 | -11.286 |
| `gc.count` | counts | 24.000 (error n/a) | 40.000 (error n/a) | 66.667 |
| `gc.time` | ms | 136.000 (error n/a) | 145.000 (error n/a) | 6.618 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1614.125 (error n/a) | 1619.750 (error n/a) | 0.348 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 2853.750 (error n/a) | 2718.750 (error n/a) | -4.731 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 8880.500 (error n/a) | 8910.875 (error n/a) | 0.342 |
| `mempool.Compressed Class Space.used` | KiB | 3147.063 (error n/a) | 3147.070 (error n/a) | 0.000 |
| `mempool.G1 Eden Space.used` | KiB | 627712.000 (error n/a) | 627712.000 (error n/a) | 0.000 |
| `mempool.G1 Old Gen.used` | KiB | 14762.414 (error n/a) | 15543.172 (error n/a) | 5.289 |
| `mempool.G1 Survivor Space.used` | KiB | 7011.422 (error n/a) | 6768.102 (error n/a) | -3.470 |
| `mempool.Metaspace.used` | KiB | 29897.219 (error n/a) | 29918.805 (error n/a) | 0.072 |
| `mempool.total.codeheap.used` | KiB | 13190.625 (error n/a) | 13156.125 (error n/a) | -0.262 |
| `mempool.total.used` | KiB | 692132.406 (error n/a) | 692453.875 (error n/a) | 0.046 |

| Percentile | Baseline us/batch | Candidate us/batch | Change % |
| --- | ---: | ---: | ---: |
| p0.0 | 1140.736 | 540.672 | -52.603 |
| p50.0 | 1779.712 | 826.368 | -53.567 |
| p90.0 | 2960.179 | 1728.512 | -41.608 |
| p95.0 | 3552.666 | 2101.248 | -40.854 |
| p99.0 | 5406.720 | 3493.315 | -35.389 |
| p99.9 | 9997.976 | 7204.749 | -27.938 |
| p99.99 | 27841.626 | 17151.171 | -38.397 |
| p99.999 | 36831.232 | 33128.448 | -10.053 |
| p99.9999 | 36831.232 | 33128.448 | -10.053 |
| p100.0 | 36831.232 | 33128.448 | -10.053 |

## sample / nested128 / 1 worker

Original results: [baseline](../baseline-5a121b67-v2/sample-t1.json), [candidate](sample-t1.json).
Primary mean intervals: [2548.754, 2636.853] / [1425.969, 1468.587] us/op.

| Metric | Unit | Baseline score ± error | Candidate score ± error | Change % |
| --- | --- | ---: | ---: | ---: |
| `primary` | us/op | 2592.803 ± 44.050 | 1447.278 ± 21.309 | -44.181 |
| `gc.alloc.rate` | MB/sec | 485.177 ± 109.952 | 812.459 ± 149.081 | 67.456 |
| `gc.alloc.rate.norm` | B/op | 1337081.253 ± 12425.237 | 1244984.222 ± 5975.056 | -6.888 |
| `gc.count` | counts | 24.000 (error n/a) | 40.000 (error n/a) | 66.667 |
| `gc.time` | ms | 155.000 (error n/a) | 224.000 (error n/a) | 44.516 |
| `mempool.CodeHeap 'non-nmethods'.used` | KiB | 1613.250 (error n/a) | 1625.500 (error n/a) | 0.759 |
| `mempool.CodeHeap 'non-profiled nmethods'.used` | KiB | 2581.125 (error n/a) | 2594.875 (error n/a) | 0.533 |
| `mempool.CodeHeap 'profiled nmethods'.used` | KiB | 9540.500 (error n/a) | 9663.625 (error n/a) | 1.291 |
| `mempool.Compressed Class Space.used` | KiB | 3148.164 (error n/a) | 3148.773 (error n/a) | 0.019 |
| `mempool.G1 Eden Space.used` | KiB | 622592.000 (error n/a) | 627712.000 (error n/a) | 0.822 |
| `mempool.G1 Old Gen.used` | KiB | 9216.000 (error n/a) | 15657.164 (error n/a) | 69.891 |
| `mempool.G1 Survivor Space.used` | KiB | 7022.016 (error n/a) | 7037.563 (error n/a) | 0.221 |
| `mempool.Metaspace.used` | KiB | 29996.289 (error n/a) | 30040.656 (error n/a) | 0.148 |
| `mempool.total.codeheap.used` | KiB | 13668.125 (error n/a) | 13741.250 (error n/a) | 0.535 |
| `mempool.total.used` | KiB | 684913.930 (error n/a) | 693347.352 (error n/a) | 1.231 |

| Percentile | Baseline us/batch | Candidate us/batch | Change % |
| --- | ---: | ---: | ---: |
| p0.0 | 1449.984 | 741.376 | -48.870 |
| p50.0 | 2256.896 | 1183.744 | -47.550 |
| p90.0 | 3735.552 | 2256.896 | -39.583 |
| p95.0 | 4308.992 | 2662.400 | -38.213 |
| p99.0 | 6676.480 | 4262.871 | -36.151 |
| p99.9 | 25893.274 | 9850.765 | -61.956 |
| p99.99 | 40897.085 | 24614.299 | -39.814 |
| p99.999 | 42795.008 | 50921.472 | 18.989 |
| p99.9999 | 42795.008 | 50921.472 | 18.989 |
| p100.0 | 42795.008 | 50921.472 | 18.989 |
