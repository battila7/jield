# Jield CPS

Generator functions in Java using CPS-transformation.

## TODO

  * Transform `foreach` loops
  * Rename local variables where necessary (Watch out for `k`! It must be renamed no to clash with the continuation parameter!)
  * Split `GeneratorTransformer` into smaller more concentrated transformers using some kind of *Visitor* pattern if possible