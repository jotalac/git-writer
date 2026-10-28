package dev.jotalac.feature.git_sync.data

import kotlin.io.encoding.Base64

/*
* Bundles made by git itself (`git bundle create`), so both implementations are tested against what git writes:
* FULL has the first commit (a.md "first note"), NEXT the second one (b.md "second note") and builds on FULL.
* */
object GitBundleFixtures {
    const val FIRST_COMMIT = "33c932f7da488d105b3cecdb155d6058a26e1198"
    const val SECOND_COMMIT = "fd707dfee68952dfd51b590cccef17530c65ed1a"

    val FULL: ByteArray = Base64.decode("IyB2MiBnaXQgYnVuZGxlCjMzYzkzMmY3ZGE0ODhkMTA1YjNjZWNkYjE1NWQ2MDU4YTI2ZTExOTggcmVmcy9oZWFkcy9tYWluCgpQQUNLAAAAAgAAAAOYB3icfcoxDoAgDEDRnVN0dynVFkyM8SogJTIYE6z31xP4pzd866qQJ/TEZZRaZKSicQ5JsfCsKU4faxbmWMmlx46rg8Fim63ggwQiFkQY8Mvt13k2M/1ZXG39NvcCDK8gzKACeJwzNDAwMzFRSNTLTWFYNfnqHjazqY7XLy5ftpbLsvRi/dZCALheDWA7eJxLyywqLlHIyy9J5QIAGh8ECSroFC+zkxd6QHHUFzQBqYZbXDK5")
    val NEXT: ByteArray = Base64.decode("IyB2MiBnaXQgYnVuZGxlCi0zM2M5MzJmN2RhNDg4ZDEwNWIzY2VjZGIxNTVkNjA1OGEyNmUxMTk4IGZpcnN0CmZkNzA3ZGZlZTY4OTUyZGZkNTFiNTkwY2NjZWYxNzUzMGM2NWVkMWEgcmVmcy9oZWFkcy9tYWluCgpQQUNLAAAAAgAAAAOZCnicfcsxDsIwDEDRPafIzmI7ceNICHGVJHYFQxtU3PvTE7D86X0/zGK33KoNrWzWO0sioq66Zq2STZQMM0OV8GmH7R5TGjXRWrRlEUXgnsa1d2TWBVgaLYZ4+Xb6ax7R492f/ohYlpKQACDerkAYc9ve7vaHhK+NuWv4AT0fLuWgBHicMzQwMDMxUUjUy01hWDX56h42s6mO1y8uX7aWy7L0Yv3WQkOIfBJIPuFhoMw2RtWU+qnGDy1naLxepnT9KgAEbhl+PHicK05Nzs9LUcjLL0nlAgAeigRdUYN5G9iRaiGDYZXvNTpEsWoN+mw=")
}
