This directory contains tests for parsing a docker containerId from Linux containers with the cgroup V2 API. 

The id is grabbed from the `proc/self/mountinfo` file in the container's internal filesystem. This file is expected to 
be missing for Linux distributions <2.6.26, which come with cgroup V1 OOTB. 

It is possible for users to manually enable cgroup V1 on newer Linux distributions, in which case the 
`proc/self/mountinfo` file will exist, and the id will be grabbed in the same manner as for cgroup V2. 
The expected contents of a manually set V1 system are shown in `docker_linux2.6.26_v1_enabled.txt`.

There is not yet a cross-agent test spec for cgroup V2 implementation, which is why these tests are currently here. 