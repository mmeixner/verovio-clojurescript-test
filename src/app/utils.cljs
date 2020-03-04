(ns app.utils)

(def dec->bin
  "Returns x as binary number in a string, '0' if no input (nil)."
  (fnil #(.toString % 2) 0))
