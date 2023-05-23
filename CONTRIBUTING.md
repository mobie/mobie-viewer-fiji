# Contributing Guidelines

First of all, thank you so much for considering to contribute to MoBIE!!!
We really appreciate all kinds of contributions:
If you find problems using MoBIE or have an idea how to improve it, please consider [opening an issue][main-repo-issues].
In case you consider contributing code to MoBIE, the following guide should get you started!


## Development setup

### Fork and clone the repository

1. Fork the repository using github's web ui.

2. Clone your fork using [git][git]:

   ```bash
   git clone https://github.com/<your_github_handle>/mobie-viewer-fiji
   ```

3. Setup the upstream remote to be able to pull in any changes from the original repo

   ```bash
   git remote add upstream https://github.com/mobie/mobie-viewer-fiji
   ```

4. Create a branch to work on
   We follow the [GitHub Flow][github-flow] where new changes are first introduced into the `develop` branch before being release on the main branch.
   If you want to work on a new feature, follow these steps:
   
   ```bash
   # go to the develop branch
   git checkout develop

   # pull in the latest changes from upstream
   git pull upstream

   # create a new branch
   git checkout -b <choose_a_good_branch_name>

   # do your changes, add and commit
   ```

### Create a conda environment with the required dependencies using [mamba][mamba]

We recommend using [mambaforge][mambaforge] to create and manage your conda environments.

```bash
mamba create -n mobie-dev -c conda-forge openjdk=8 maven
```

### Build and test

```bash
# make sure to be in the conda development environment
mamba activate mobie-dev
maven test

# if you want the commands available for testing, running
./install.sh
```

### Open a pull request

After testing your changes locally and making sure all updated files have been committed, you can push your branch:

```bash
git push --set-upstream origin <your_branch_name>
```

Go to the [main repository to open a pull request][main-repo-pulls].
Make sure to use the `develop` branch as a base.


[git]: https://git-scm.com/
[github-flow]: https://guides.github.com/introduction/flow/
[main-repo-issues]: https://github.com/mobie/mobie-viewer-fiji/issues/new
[main-repo-pulls]: https://github.com/mobie/mobie-viewer-fiji/pulls
[mamba]: https://mamba.readthedocs.io/en/latest/
[mambaforge]: https://github.com/conda-forge/miniforge#mambaforge