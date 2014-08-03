memory - large memory access from Java
===
ByteBuffer has a few major drawbacks by design:
 1. Builtin position counter which makes quite hard to pass instancies around without "duplicate". Duplicate leads to a lot of garbage generation
 2. Int indexing, so it is impossible to mmap more then 2GB even in native code
 3. Explicit bounds chacking and order byte conversations. That could be quite slow sometimes
 4. It is class that can't be extended/override in many ways (sic!)
 5. Ugly readonly design
 6. Really ugly typed write/read design. For fast Int write u need to cast to IntBuffer and then track separate position counter....


Memory package introduce MemoryAccess interface instead with:
 1. Clean read/write interface separation
 2. CAS instructions
 3. Clean flat hierarchy 
 4. Default fast implementation over ByteBuffer/byte arrays
 5. long indexing schema

With interfaces it is possible to write decorators to proxy memory access, create adaptors to create "continues" views over multiple memory chunks, e.t.c

Note: That is "open source" version of internal library I wrote several years ago. I have to cut out & replace a lot of proprietary code, so library could be unstable for a some time

Note2: mmap parts will be much later
