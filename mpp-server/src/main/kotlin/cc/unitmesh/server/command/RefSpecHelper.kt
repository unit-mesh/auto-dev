/*
 * Copyright 2022 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cc.unitmesh.server.command

object RefSpecHelper {
    const val REFS_HEADS = "refs/heads/"
    const val REFS_REMOTES = "refs/remotes/"

    fun localBranch(branch: String): String {
        val local = findDest(branch) ?: return branch
        if (local.startsWith(REFS_HEADS)) {
            return local.substring(REFS_HEADS.length)
        }
        if (local.startsWith(REFS_REMOTES)) {
            val bound = local.indexOf("/", REFS_REMOTES.length)
            // If the user does not specify a branch under the remote, this is likely
            // a user error. As a failsafe, allow the condition to fall through, which
            // effectively returns `refs/remotes/<remote-name>` as this will be
            // resolvable in git.
            if (-1 != bound) {
                return local.substring(bound + 1)
            }
        }
        return local
    }

    fun remoteBranch(branch: String): String {
        val local = findDest(branch) ?: return "origin/$branch"
        if (!local.startsWith("refs/")) {
            return REFS_HEADS + local
        }
        if (local.startsWith(REFS_REMOTES)) {
            // If the user does not specify a branch under the remote, this is likely
            // a user error. As a failsafe, allow the condition to fall through, which
            // effectively returns `refs/remotes/<remote-name>` as this will be
            // resolvable in git.
            if (-1 != local.indexOf("/", REFS_REMOTES.length)) {
                return local.substring(REFS_REMOTES.length)
            }
        }
        return local
    }

    /**
     * Finds the full ref of the upstream branch; for refSpecs, this returns the source fragment.
     *
     *
     * This is mainly used for `git ls-remote` during git connection check.
     *
     * @return the full ref of the upstream branch or source fragment of the refSpec
     */
    fun fullUpstreamRef(branch: String): String {
        val source = findSource(branch)
        return source ?: REFS_HEADS + branch
    }

    fun hasRefSpec(branch: String): Boolean {
        return -1 != refSpecBoundary(branch)
    }

    /**
     * Ensures that the refSpec destination has an absolute path
     *
     * @return the absolute refSpec
     */
    fun expandRefSpec(branch: String): String {
        val source = findSource(branch)
            ?: // equiv to hasRefSpec()
            return branch
        val dest = findDest(branch)
        return if (null == dest || dest.startsWith("refs/")) {
            branch
        } else source + ":" + REFS_REMOTES + "origin/" + dest

        // NOTE: This behavior differs from the `git fetch <remote> <refSpec>` implicit, default
        // expansion, which effectively interprets `refs/a/b:c` as `refs/a/b:refs/heads/c`.
        //
        // Expanding the destination to be under `refs/remotes/origin/<dest>` is a more sensible
        // default for how GoCD works. As we actually create and _switch_ to the branch named
        // by the destination, `git fetch` would *fail* if the refSpec destination were to be
        // `refs/heads/<branchName>`; fetching directly to the current branch is illegal in `git`
        // (HEAD actually points to `refs/heads/<branchName>`).
        //
        // Fetching to `refs/remotes/origin/<branchName>` (and then merging, à la "pull") works
        // perfectly fine from the current branch.
        //
        //   -- In case you were wondering. 🖖🏼
    }

    fun findSource(branch: String): String? {
        val boundary = refSpecBoundary(branch)
        return if (-1 == boundary) null else branch.substring(0, boundary)
    }

    fun findDest(branch: String): String? {
        val boundary = refSpecBoundary(branch)
        return if (-1 == boundary) null else branch.substring(boundary + 1)
    }

    private fun refSpecBoundary(branch: String): Int {
        return branch.indexOf(':')
    }
}
