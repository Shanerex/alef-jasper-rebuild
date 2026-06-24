-- V3: Update project image paths from .jpg placeholders to .svg placeholders.
-- The seed (V2) referenced .jpg files that don't exist; actual placeholders are SVGs.

UPDATE project SET image = REPLACE(image, '.jpg', '.svg') WHERE image LIKE '%.jpg';
