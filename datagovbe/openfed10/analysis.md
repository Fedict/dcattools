# Content type and taxonomy analysis

## Taxonomy: Application Types

| Field | Type | Mandatory | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |

## Taxonomy: Contact Types

| Field | Type | Mandatory | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |

## Taxonomy: File Types

Language-independent

| Field | Type | Mandatory |
| --- | --- | --- |
| Title | String | yes |
| URI | Link | yes |

## Taxonomy: Geographies

| Field | Type | Mandatory | Translatable |
| --- | --- | --- | --- |
| Title | String | yes  | yes |
| Box | Geo | yes | no |
| URI | Link | yes | no |

## Taxonomy: Licenses

| Field | Type | Mandatory | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |
| Abbreviation | String | no | yes |
| URI | Link | yes | no |

## Taxonomy: Organizations

| Field | Type | Mandatory | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |
| Abbreviation | String | no | yes |
| URI | Link | yes | no |

## Content type: Application

| Field | Type | Mandatory | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |
| Description | Text field | yes | yes |
| Image | Image | no | no |
| Website | Link | yes | yes |


## Contact form

| Field | Type | Mandatory |
| --- | --- | --- |
| Your name | String | --- |
| Your e-mail | Email | yes |
| Category | Taxonomy: Contact Type | --- |
| Subject | String | yes |
| Message | Text field | yes |
| Agree policy | Boolean | yes |
