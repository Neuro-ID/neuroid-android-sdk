# Generating a PGP Signing Key for Maven Central Publishing

Maven Central requires every published artifact to be signed with a PGP key whose
public key is discoverable on a public keyserver. This project's `NeuroID/build.gradle`
uses Gradle's `signing` plugin with `useInMemoryPgpKeys(signingInMemoryKey, signingInMemoryKeyPassword)`,
which expects the **private** key material and its passphrase to be supplied via
Gradle properties (or `SIGNING_KEY` / `SIGNING_PASSWORD` env vars in CI).

This doc covers generating that key pair and formatting it for a GitHub Actions secret.

## 1. Install GPG

```
brew install gnupg
```

## 2. Generate a new key pair

```
gpg --full-generate-key
```

- Kind: `RSA and RSA` (default)
- Key size: `4096`
- Expiration: your call (e.g. `2y`, or `0` for no expiration)
- Real name / email: use a team-owned identity (e.g. `NeuroID Release <releases@neuro-id.com>`),
  not a personal email, so the key isn't tied to one person leaving the team.
- Set a strong passphrase — this is the value for `signingInMemoryKeyPassword`.

## 3. Find the key ID

```
gpg --list-secret-keys --keyid-format=long
```

Look for a line like:
```
sec   rsa4096/AB12CD34EF56AB78 2026-09-22 [SC]
```
`AB12CD34EF56AB78` is the key ID (last 16 chars, no spaces).

## 4. Publish the public key to a keyserver

Maven Central verifies the signature's public key is retrievable. Publish to at least one
of the keyservers it checks:

```
gpg --keyserver keyserver.ubuntu.com --send-keys AB12CD34EF56AB78
gpg --keyserver keys.openpgp.org --send-keys AB12CD34EF56AB78
```

## 5. Export the private key (this is `signingInMemoryKey`)

```
gpg --export-secret-keys --armor AB12CD34EF56AB78 > private-key.asc
```

Open `private-key.asc` — it must look like:
```
-----BEGIN PGP PRIVATE KEY BLOCK-----

lQdFBGpahHsBEAC5xFRsbDtsQmaoTOW2P9vw1ud8buQRIaQKWRv4HNNap4lzhxFD
...
-----END PGP PRIVATE KEY BLOCK-----
```

## 6. Add it to GitHub Secrets

GitHub Actions secrets support real multi-line values directly — paste the **entire
file contents** (headers included) as-is into the secret value box. No need to escape
newlines as `\n` when using GitHub Secrets + env vars (that escaping is only needed if
hand-editing a single-line `.properties` file).

In your repo: **Settings → Secrets and variables → Actions → New repository secret**

| Secret name    | Value                                  |
|-----------------|-----------------------------------------|
| `SIGNING_KEY`      | full contents of `private-key.asc`   |
| `SIGNING_PASSWORD` | the passphrase from step 2            |

These map directly to the env vars already read by `NeuroID/build.gradle`'s signing
config (`SIGNING_KEY` / `SIGNING_PASSWORD`, falling back to the
`signingInMemoryKey` / `signingInMemoryKeyPassword` Gradle properties for local use).

## 7. Reference in your GitHub Actions workflow

```yaml
- name: Publish to Maven Central
  env:
    SIGNING_KEY: ${{ secrets.SIGNING_KEY }}
    SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
    SONATYPE_USERNAME: ${{ secrets.SONATYPE_USERNAME }}
    SONATYPE_PASSWORD: ${{ secrets.SONATYPE_PASSWORD }}
  run: ./gradlew publishToMavenCentral
```

## 8. Clean up local copies

```
rm private-key.asc
```

Do not commit the exported key file or paste it into `gradle.properties` for anything
other than local, throwaway testing.

## Rotating the key

If the key is ever leaked or compromised:
```
gpg --keyserver keyserver.ubuntu.com --send-keys --revoke-keys AB12CD34EF56AB78
```
Then generate a new key pair (steps 2–6) and update the GitHub secrets. Already-published
artifacts on Maven Central keep their original signatures and remain valid; only future
publishes need the new key.

