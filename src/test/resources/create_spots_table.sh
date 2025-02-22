  #!/bin/bash

  # Number of rows to generate
  NUM_ROWS=70000

  # Print the header
  echo "spot_id\tx\ty\tz\tgene"

  # Generate the data
  awk -v num_rows="$NUM_ROWS" 'BEGIN {
      # Seed the random number generator for reproducibility
      srand();

      # Initialize gene characters
      gene_chars = "abcdefghijklmnopqrstuvwxyz";

      for (i = 1; i <= num_rows; i++) {
          # Generate random coordinates
          x = rand() * 160;
          y = rand() * 80;
          z = rand() * 5;

          # Select a random gene character
          gene = substr(gene_chars, int(rand() * length(gene_chars)) + 1, 1);

          # Print the row
          printf("%d\t%.1f\t%.1f\t%.2f\t%s\n", i, x, y, z, gene);
      }
  }'