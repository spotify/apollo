# Contributing

We love pull requests. Here's a quick guide.

### 1. Fork, then clone the repo:

    git clone git@github.com:spotify/apollo.git

### 2. Install dependencies & build:

    mvn package

### 3. Write your feature

Ok, so now you can write some code. Have fun!

### 4. Add tests for your feature. Make the tests pass:

    mvn -Pcoverage verify
    
### 5. Write documentation

Make sure to document your feature in the relevant places (Javadoc comments, README files, etc).

### 6. Push to your fork and [submit a pull request][pr].

Make sure the PR text mentions the motivation and details for your change.

[pr]: https://github.com/spotify/apollo/compare/

CircleCI will run the test suite and let you know that you've done an awesome job. If CircleCI says A-OK, your work is done for now. At this point you're waiting on us. We like to at least comment on pull requests
within two business days (and, typically, one business day). We may suggest
some changes or improvements or alternatives. We promise to respond to all PRs.
