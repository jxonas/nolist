(ns nolist.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [nolist.core-test]
   [nolist.common-test]))

(enable-console-print!)

(doo-tests 'nolist.core-test
           'nolist.common-test)
