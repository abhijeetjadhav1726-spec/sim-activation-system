# Jenkins on Windows — setup guide

This guide is for the workstation where this repository lives. The Jenkinsfile uses Windows `bat` and `powershell` steps. It expects Git, JDK, Maven, Docker CLI/daemon, and Chrome on the Jenkins build agent.

## 1. Install and verify prerequisites

1. Install a supported 64-bit JDK. For a current Jenkins LTS, install a Jenkins-supported runtime (Java 21 is a practical choice; follow the selected release's requirement). The project targets Java 17 via `java.version` in `pom.xml`, so Maven can compile it for Java 17 while running on JDK 21. Install JDK 17 too only if you specifically need CI tests to execute on Java 17.
2. Install Git for Windows, Maven 3.9+, Docker Desktop, and Chrome. Keep their installation paths stable.
3. Open a new PowerShell window and verify:

   ```powershell
   java -version
   javac -version
   mvn -version
   git --version
   docker version
   ```

   `docker version` must show both a Client and Server section. Start Docker Desktop and wait until its engine is running before continuing.

4. Set `JAVA_HOME` to the JDK directory (not its `bin` directory), and add `%JAVA_HOME%\bin` and Maven's `bin` folder to the system `Path`. Restart terminals after changing environment variables.

## 2. Install Jenkins

1. Download the official Windows Jenkins installer from [jenkins.io/download](https://www.jenkins.io/download/). Prefer the stable/LTS installer.
2. Run the installer as an administrator, select the supported Java runtime when prompted, and keep the default controller port `8080` unless another service uses it. The SIM app also defaults to `8080`; they cannot both listen on the same host port at once. Jenkins can use its default 8080 while the app uses 18080 locally, or set Jenkins to another port during setup.
3. Complete the unlock wizard at `http://localhost:<jenkins-port>` using the initial administrator password path shown by the wizard. Install the suggested plugins.
4. Create an admin user, set the Jenkins URL to `http://localhost:<jenkins-port>/`, and confirm the dashboard loads.
5. Install or verify the **Pipeline**, **Git**, **Credentials Binding**, **JUnit**, and **PowerShell** plugins in **Manage Jenkins → Plugins**. Restart Jenkins if the plugin manager requests it.

## 3. Make the Jenkins build agent see the tools

1. For a single-device training setup, start by running a build and check the Jenkins console for:

   ```bat
   java -version
   mvn -version
   git --version
   docker version
   ```

2. A Windows Jenkins service may run as `LocalSystem`, which does not necessarily share your interactive user's PATH, Chrome profile, or Docker Desktop daemon access. If tools are not found, configure the Jenkins service under **Services → Jenkins → Properties → Log On** to use a dedicated local build account, set system `JAVA_HOME`/`PATH`, and restart the service.
3. If Docker is inaccessible from the service, run a Jenkins agent in the logged-in Windows user session with access to the Docker Desktop engine, and assign the Docker build stage to that agent label. Do not grant Docker access to untrusted users: Docker daemon access is effectively administrator-level.
4. Verify Docker from the exact agent that will run the job; a successful `docker version` in your own terminal alone does not prove Jenkins has daemon access.

## 4. Add the GitHub pipeline job

The configured repository is `https://github.com/abhijeetjadhav1726-spec/sim-activation-system.git`.

1. In Jenkins select **New Item → Multibranch Pipeline**, name it `sim-activation-system`, and select **OK**.
2. Under **Branch Sources**, add **Git** and enter that repository URL. If it is private, add a Jenkins credential using GitHub's recommended fine-grained access token; do not paste the token into the Jenkinsfile or source code.
3. Set the script path to `Jenkinsfile`; leave build strategies at defaults for the first run. Save and select **Scan Multibranch Pipeline Now**.
4. Jenkins should discover `main` and create a branch job. Open it and select **Build Now**. Inspect the console output and archived artifact for a passing `mvn -B clean test` and packaged JAR.
5. On GitHub, protect `main` and require the Jenkins result before merging. A GitHub webhook cannot reach a Jenkins server bound only to localhost. For this single-PC setup, use periodic multibranch scanning/polling; use a properly secured, reachable Jenkins endpoint before configuring a webhook. Do not expose a home machine's Jenkins port directly to the public internet.

## 5. Configure optional browser, registry, and deployment stages

- **Selenium:** Install Chrome on the Jenkins agent and verify that Chrome can run in headless mode. Selenium Manager downloads/resolves a compatible driver; the agent needs network access to driver metadata/downloads. Run `mvn -Pselenium verify` once on the agent before enabling the main-branch stage.
- **Docker image:** The `main` branch builds `sim-activation:<BUILD_NUMBER>`. Verify the Jenkins agent can run `docker build` before relying on this stage.
- **Docker Hub (optional):** Create a Jenkins username/password credential with ID `dockerhub`. Set `PUBLISH_IMAGE=true` and `DOCKER_IMAGE=<your-dockerhub-account>/sim-activation` when running the main branch. Never store registry credentials in Git.
- **Remote deploy (optional):** Only after a Linux Docker host is authorized and reachable, add the SSH private-key credential with ID `sim-deploy-key`, set `DEPLOY_HOST` and `DEPLOY_USER`, and enable publishing. The host must be able to pull the chosen registry image. Keep these parameters empty/false for local CI-only use.

## 6. Run locally and troubleshoot port conflicts

- The app defaults to port 8080. The Selenium profile uses port 18080 by default so it can run beside Jenkins on port 8080:

  ```powershell
  mvn -Pselenium verify
  ```

- For a different free port, override it with `mvn -Pselenium -Dselenium.server.port=18081 verify`. Selenium is configured to browse the app port, not the Jenkins port.
- If `mvn` or `java` is not recognized, correct system PATH/JAVA_HOME and restart Jenkins so the service reloads the environment.
- If Docker works in PowerShell but not Jenkins, check the build agent's service identity, Docker Desktop status, named-pipe permissions, and whether the agent is running in the interactive user's session.
- If a build fails, use the branch job's **Console Output** and test reports under `target/surefire-reports/` or `target/failsafe-reports/` to identify the failing stage.
