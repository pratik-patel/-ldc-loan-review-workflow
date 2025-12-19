variable "environment" {
  description = "Environment name"
  type        = string
}

variable "dependencies_layer_path" {
  description = "Path to the dependencies layer ZIP file"
  type        = string
}

variable "shared_layer_path" {
  description = "Path to the shared code layer ZIP file"
  type        = string
}
