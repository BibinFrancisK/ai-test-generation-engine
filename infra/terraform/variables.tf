variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "region" {
  description = "AWS region"
  type = string
  default = "us-east-1"
}

variable "local_endpoint" {
  description = "Endpoint to direct locally emulated API calls to"
  type = string
  default = "http://localhost:4566"
}

variable "project" {
  description = "The name of the project"
  type = string
  default = "ai-test-generation-engine"
}