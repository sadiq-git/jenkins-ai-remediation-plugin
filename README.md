🤖 Jenkins AI Remediation Plugin

Autonomous Pipeline Diagnosis & Repair powered by Groq Llama 3

This Jenkins plugin automatically intercepts pipeline failures, analyzes the build logs using a Large Language Model (Groq/Llama 3), and proactively generates a fixed version of your code. It provides a Human-in-the-Loop (HITL) interface where you can review the AI's proposed fix before one-click pushing it to a new GitHub repository.

🧠 Logical Workflow

```mermaid
graph TD
    %% Nodes
    Build[Pipeline Failure]
    Listener(AiRemediationListener)
    Sanitizer{LogSanitizer}
    AI_Service[AiDiagnosticService]
    LLM[Groq API / Llama 3]
    Dashboard[Review Dashboard UI]
    GitService[GitHubRemediationService]
    GitHub[GitHub]

    %% Edge Connections
    Build -->|Triggers| Listener
    Listener -->|Extracts Last 150 Lines| Sanitizer
    Sanitizer -->|Scrub Secrets| AI_Service
    AI_Service -->|Prompt Engineering| LLM
    LLM -->|JSON Response| AI_Service
    AI_Service -->|Fix & Explanation| Listener
    Listener -->|Attach Action| Dashboard
    
    subgraph Human_in_the_Loop[Human in the Loop]
        Dashboard -->|Displays Diff| User((Developer))
        User -->|Clicks Create Repo Push| GitService
    end

    GitService -->|Create Repo Commit| GitHub
    GitHub -->|Return New Repo URL| Dashboard
    
    %% Styling
    style Build fill:#ffcccc,stroke:#333,stroke-width:2px
    style LLM fill:#e6f3ff,stroke:#333,stroke-width:2px
    style GitHub fill:#f0f0f0,stroke:#333,stroke-width:2px
    style Dashboard fill:#e6ffe6,stroke:#333,stroke-width:2px
```

✨ Features

Auto-Detection: Automatically catches Result.FAILURE events in Pipelines.

Secure Analysis: Logs are sanitized (secrets masked) before sending to the AI.

Structured Output: Forces the AI to return structured JSON (Explanation, FileName, FixedCode).

Visual Diff: Integrated diff2html viewer to compare original vs. AI-suggested code.

Automated Git Ops: Creates a brand new repository (e.g., jenkins-job-fail-test) and commits the fix with a single click.

🛠️ Configuration

Prerequisites

Jenkins: Version 2.479.3 or newer (Jakarta EE support).

Java: JDK 17 or newer.

Global Setup

Navigate to Manage Jenkins > Credentials.

Add Groq API Key (Secret Text).

Add GitHub Personal Access Token (Secret Text, must have repo scope).

Navigate to Manage Jenkins > System.

Scroll to AI Remediation Plugin.

Select your Groq Credential ID.

Select your GitHub Credential ID.

(Optional) Set the model name (default: llama-3.3-70b-versatile).

🚀 Development

Build & Run

# Clean build to ensure Jakarta dependencies are resolved
mvn clean hpi:run -Dport=8090


Installation

Run mvn clean package.

Upload target/ai-remediation.hpi to your Jenkins instance via Manage Jenkins > Plugins > Advanced.

🔒 Security Note

This plugin sends build logs to an external API (Groq). While the LogSanitizer attempts to scrub patterns resembling keys and tokens, always review what data your pipelines output to the console.